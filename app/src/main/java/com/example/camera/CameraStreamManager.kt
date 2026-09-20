package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.ResolutionPreset
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraStreamManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onFrameAvailable: (ByteArray) -> Unit
) {
    private val TAG = "CameraStreamManager"
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Reusable byte output stream pre-sized to 256KB to avoid reallocations
    private val reusableStream = ByteArrayOutputStream(256 * 1024)

    // Reusable transformation resources to achieve zero-allocation per frame
    private var transformedBitmap: Bitmap? = null
    private var transformCanvas: Canvas? = null
    private val transformMatrix = Matrix()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var lastTransformedWidth = 0
    private var lastTransformedHeight = 0

    // Atomic guard to ensure we never queue up frame processing
    private val isEncoding = AtomicBoolean(false)

    @Volatile
    private var targetFps: Int = 30

    @Volatile
    private var jpegQuality: Int = 65

    @Volatile
    private var resolutionPreset: ResolutionPreset = ResolutionPreset.HD_720P

    @Volatile
    private var isBackCamera: Boolean = true

    @Volatile
    private var isTorchOn: Boolean = false

    private var lastFrameTimeMs: Long = 0L
    private val isStreamingActive = AtomicBoolean(false)
    private var previewView: PreviewView? = null

    fun setStreamingActive(active: Boolean) {
        isStreamingActive.set(active)
    }

    fun updateSettings(
        preset: ResolutionPreset,
        fps: Int,
        quality: Int,
        backCamera: Boolean
    ) {
        val resolutionChanged = this.resolutionPreset != preset
        val cameraFacingChanged = this.isBackCamera != backCamera
        this.resolutionPreset = preset
        this.targetFps = fps.coerceIn(5, 60)
        this.jpegQuality = quality.coerceIn(10, 100)
        this.isBackCamera = backCamera

        if (resolutionChanged || cameraFacingChanged) {
            previewView?.let { bindCameraUseCases(it) }
        }
    }

    fun setTargetFps(fps: Int) {
        this.targetFps = fps.coerceIn(5, 60)
    }

    fun setJpegQuality(quality: Int) {
        this.jpegQuality = quality.coerceIn(10, 100)
    }

    fun setResolution(preset: ResolutionPreset) {
        if (this.resolutionPreset != preset) {
            this.resolutionPreset = preset
            previewView?.let { bindCameraUseCases(it) }
        }
    }

    fun switchCamera() {
        isBackCamera = !isBackCamera
        isTorchOn = false
        previewView?.let { bindCameraUseCases(it) }
    }

    fun setTorch(enable: Boolean) {
        if (!isBackCamera && enable) return
        isTorchOn = enable
        camera?.cameraControl?.enableTorch(enable)
    }

    fun focusOnPoint(x: Float, y: Float, width: Float, height: Float) {
        try {
            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(width, height)
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point).build()
            camera?.cameraControl?.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.e(TAG, "Focus error: ${e.message}")
        }
    }

    fun bindCameraUseCases(previewView: PreviewView) {
        this.previewView = previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val provider = cameraProvider ?: return@addListener

                provider.unbindAll()

                val cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                if (!provider.hasCamera(cameraSelector)) {
                    Log.e(TAG, "Selected camera lens not available on device")
                    return@addListener
                }

                // Setup Preview
                preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Setup ImageAnalysis using RGBA_8888 for high-speed direct bitmap conversion
                val targetSize = Size(resolutionPreset.width, resolutionPreset.height)
                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(targetSize)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            processImageProxy(imageProxy)
                        }
                    }

                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                if (isBackCamera && isTorchOn) {
                    camera?.cameraControl?.enableTorch(true)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            if (!isStreamingActive.get()) {
                imageProxy.close()
                return
            }

            val now = System.currentTimeMillis()
            // 85% jitter tolerance window: Prevents camera frames arriving at 31-32ms from being dropped
            val minIntervalMs = (1000.0 / targetFps * 0.85).toLong()
            if (now - lastFrameTimeMs < minIntervalMs) {
                imageProxy.close()
                return
            }

            // Drop frame if previous encoding is still busy to prevent pipeline stalls
            if (!isEncoding.compareAndSet(false, true)) {
                imageProxy.close()
                return
            }

            lastFrameTimeMs = now

            val sourceBitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val isFront = !isBackCamera

            val finalBitmapToEncode = if (rotationDegrees != 0 || isFront) {
                // Calculate destination dimensions
                val dstWidth = if (rotationDegrees == 90 || rotationDegrees == 270) sourceBitmap.height else sourceBitmap.width
                val dstHeight = if (rotationDegrees == 90 || rotationDegrees == 270) sourceBitmap.width else sourceBitmap.height

                // Allocate destination bitmap and canvas once per resolution change
                if (transformedBitmap == null || dstWidth != lastTransformedWidth || dstHeight != lastTransformedHeight) {
                    transformedBitmap?.recycle()
                    transformedBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
                    transformCanvas = Canvas(transformedBitmap!!)
                    lastTransformedWidth = dstWidth
                    lastTransformedHeight = dstHeight
                }

                transformMatrix.reset()

                if (isFront) {
                    // Mirror horizontally
                    transformMatrix.postScale(-1f, 1f, sourceBitmap.width / 2f, sourceBitmap.height / 2f)
                }

                if (rotationDegrees != 0) {
                    transformMatrix.postRotate(rotationDegrees.toFloat())
                    // Translate back into viewport
                    when (rotationDegrees) {
                        90 -> transformMatrix.postTranslate(dstWidth.toFloat(), 0f)
                        180 -> transformMatrix.postTranslate(dstWidth.toFloat(), dstHeight.toFloat())
                        270 -> transformMatrix.postTranslate(0f, dstHeight.toFloat())
                    }
                }

                val canvas = transformCanvas
                if (canvas != null && transformedBitmap != null) {
                    canvas.drawColor(Color.BLACK)
                    canvas.drawBitmap(sourceBitmap, transformMatrix, paint)
                    transformedBitmap!!
                } else {
                    sourceBitmap
                }
            } else {
                sourceBitmap
            }

            // High-speed JPEG compression directly to reusable byte buffer
            synchronized(reusableStream) {
                reusableStream.reset()
                finalBitmapToEncode.compress(Bitmap.CompressFormat.JPEG, jpegQuality, reusableStream)
                val jpegBytes = reusableStream.toByteArray()
                onFrameAvailable(jpegBytes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
        } finally {
            isEncoding.set(false)
            imageProxy.close()
        }
    }

    fun release() {
        try {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
            transformedBitmap?.recycle()
            transformedBitmap = null
            transformCanvas = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera: ${e.message}")
        }
    }
}
