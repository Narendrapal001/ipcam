package com.example.server

import android.util.Log
import com.example.model.ResolutionPreset
import com.example.model.StreamQualitySettings
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class MjpegHttpServer(
    private val port: Int,
    private val getSettings: () -> StreamQualitySettings,
    private val getLocalIp: () -> String,
    private val onToggleTorch: (Boolean) -> Unit,
    private val onFlipCamera: () -> Unit,
    private val onQualityChange: (ResolutionPreset) -> Unit,
    private val onFpsChange: (Int) -> Unit,
    private val onClientCountChanged: (Int) -> Unit
) {
    private val TAG = "MjpegHttpServer"
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()

    // Bounded client queues decouple camera capture from network socket I/O
    private class ClientSession(
        val id: String,
        val queue: ArrayBlockingQueue<ByteArray> = ArrayBlockingQueue(2)
    )
    private val activeSessions = ConcurrentHashMap<String, ClientSession>()
    private val clientCounter = AtomicInteger(0)

    @Volatile
    var lastFrame: ByteArray? = null
        private set

    // Performance metrics
    private val frameCount = AtomicLong(0L)
    private val bytesSentInLastSecond = AtomicLong(0L)
    private var lastFpsCalculationTime = System.currentTimeMillis()
    private var framesInLastSecond = 0

    @Volatile
    var currentFps: Double = 0.0
        private set

    @Volatile
    var currentBitrateKbps: Double = 0.0
        private set

    fun start() {
        if (isRunning.getAndSet(true)) return

        executor.execute {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                Log.i(TAG, "Server started successfully on port $port")

                while (isRunning.get() && serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        executor.execute { handleClientConnection(clientSocket) }
                    } catch (e: SocketException) {
                        if (!isRunning.get()) break
                        Log.e(TAG, "SocketException in accept loop: ${e.message}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception in accept loop: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server: ${e.message}", e)
                isRunning.set(false)
            }
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket: ${e.message}")
        }
        serverSocket = null

        activeSessions.clear()
        onClientCountChanged(0)
    }

    /**
     * Non-blocking broadcast: places the encoded JPEG into each client's queue.
     * Completes in microseconds and NEVER blocks camera processing!
     */
    fun broadcastFrame(jpegBytes: ByteArray) {
        lastFrame = jpegBytes
        frameCount.incrementAndGet()

        framesInLastSecond++
        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsCalculationTime
        if (elapsed >= 1000) {
            currentFps = (framesInLastSecond * 1000.0) / elapsed
            val bytes = bytesSentInLastSecond.get()
            currentBitrateKbps = ((bytes * 8.0) / 1024.0) / (elapsed / 1000.0)
            framesInLastSecond = 0
            bytesSentInLastSecond.set(0L)
            lastFpsCalculationTime = now
        }

        if (activeSessions.isEmpty()) return

        for ((_, session) in activeSessions) {
            // Keep queue fresh: if full, discard older frame and insert latest
            if (session.queue.remainingCapacity() == 0) {
                session.queue.poll()
            }
            session.queue.offer(jpegBytes)
        }
    }

    private fun handleClientConnection(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.sendBufferSize = 256 * 1024
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val rawOutput = socket.getOutputStream()
            val out = BufferedOutputStream(rawOutput, 64 * 1024)

            val requestLine = reader.readLine() ?: run {
                socket.close()
                return
            }

            // Read headers
            val headers = mutableMapOf<String, String>()
            var contentLength = 0
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx != -1) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
                line = reader.readLine()
            }

            // Read body if present
            var body = ""
            if (contentLength > 0) {
                val bodyChars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val count = reader.read(bodyChars, read, contentLength - read)
                    if (count == -1) break
                    read += count
                }
                body = String(bodyChars, 0, read)
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return
            }

            val method = parts[0].uppercase()
            val uri = parts[1].split("?")[0]

            when {
                // Serve Web Client HTML
                method == "GET" && (uri == "/" || uri == "/index.html") -> {
                    val html = WebClientHtml.generateHtml(getSettings(), getLocalIp(), port)
                    val htmlBytes = html.toByteArray(Charsets.UTF_8)
                    val responseHeader = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n" +
                            "Content-Length: ${htmlBytes.size}\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(responseHeader.toByteArray())
                    out.write(htmlBytes)
                    out.flush()
                    socket.close()
                }

                // Dedicated MJPEG Streaming Loop running on its own background thread
                method == "GET" && (uri == "/stream" || uri == "/video_feed") -> {
                    val streamHeader = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Cache-Control: no-cache, no-store, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(streamHeader.toByteArray())
                    out.flush()

                    val clientId = "client_${clientCounter.incrementAndGet()}"
                    val session = ClientSession(clientId)
                    activeSessions[clientId] = session
                    onClientCountChanged(activeSessions.size)

                    // Send initial frame if available
                    lastFrame?.let { session.queue.offer(it) }

                    val footer = "\r\n".toByteArray()

                    try {
                        while (isRunning.get() && !socket.isClosed && !socket.isOutputShutdown) {
                            val frame = session.queue.poll(500, TimeUnit.MILLISECONDS) ?: continue

                            val header = ("--frame\r\n" +
                                    "Content-Type: image/jpeg\r\n" +
                                    "Content-Length: ${frame.size}\r\n\r\n").toByteArray()

                            out.write(header)
                            out.write(frame)
                            out.write(footer)
                            out.flush()

                            bytesSentInLastSecond.addAndGet(frame.size.toLong())
                        }
                    } catch (e: Exception) {
                        // Client closed browser window or tab
                    } finally {
                        activeSessions.remove(clientId)
                        onClientCountChanged(activeSessions.size)
                        try {
                            socket.close()
                        } catch (ignored: Exception) {}
                    }
                }

                // Snapshot JPEG
                method == "GET" && uri == "/snapshot" -> {
                    val frame = lastFrame
                    if (frame != null) {
                        val responseHeader = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: image/jpeg\r\n" +
                                "Content-Length: ${frame.size}\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Cache-Control: no-cache\r\n" +
                                "Connection: close\r\n\r\n"
                        out.write(responseHeader.toByteArray())
                        out.write(frame)
                        out.flush()
                    } else {
                        val notReady = "No frame captured yet".toByteArray()
                        val responseHeader = "HTTP/1.1 503 Service Unavailable\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: ${notReady.size}\r\n" +
                                "Connection: close\r\n\r\n"
                        out.write(responseHeader.toByteArray())
                        out.write(notReady)
                        out.flush()
                    }
                    socket.close()
                }

                // API: Status
                method == "GET" && uri == "/api/status" -> {
                    val settings = getSettings()
                    val json = """
                        {
                            "isStreaming": true,
                            "clients": ${activeSessions.size},
                            "fps": $currentFps,
                            "bitrateKbps": $currentBitrateKbps,
                            "resolutionTitle": "${settings.resolution.title}",
                            "resolutionWidth": ${settings.resolution.width},
                            "resolutionHeight": ${settings.resolution.height},
                            "targetFps": ${settings.targetFps},
                            "torch": ${settings.isTorchOn},
                            "isBackCamera": ${settings.isBackCamera}
                        }
                    """.trimIndent()
                    sendJsonResponse(out, json)
                    socket.close()
                }

                // API: Torch Toggle
                method == "POST" && uri == "/api/torch" -> {
                    val currentTorch = getSettings().isTorchOn
                    val newTorch = if (body.contains("\"enabled\":true") || body.contains("\"enabled\": true")) {
                        true
                    } else if (body.contains("\"enabled\":false") || body.contains("\"enabled\": false")) {
                        false
                    } else {
                        !currentTorch
                    }
                    onToggleTorch(newTorch)
                    sendJsonResponse(out, """{"success": true, "torch": $newTorch}""")
                    socket.close()
                }

                // API: Flip Camera
                method == "POST" && uri == "/api/flip" -> {
                    onFlipCamera()
                    val settings = getSettings()
                    sendJsonResponse(out, """{"success": true, "isBackCamera": ${settings.isBackCamera}}""")
                    socket.close()
                }

                // API: Change Quality
                method == "POST" && uri == "/api/quality" -> {
                    val preset = when {
                        body.contains("FHD_1080P") -> ResolutionPreset.FHD_1080P
                        body.contains("HD_720P") -> ResolutionPreset.HD_720P
                        body.contains("SD_480P") -> ResolutionPreset.SD_480P
                        body.contains("LD_360P") -> ResolutionPreset.LD_360P
                        else -> null
                    }
                    if (preset != null) {
                        onQualityChange(preset)
                        sendJsonResponse(out, """{"success": true, "resolution": "${preset.title}"}""")
                    } else {
                        sendJsonResponse(out, """{"error": "Invalid resolution"}""", 400)
                    }
                    socket.close()
                }

                // API: Change FPS
                method == "POST" && uri == "/api/fps" -> {
                    val fpsMatch = Regex("\"fps\"\\s*:\\s*(\\d+)").find(body)
                    val fps = fpsMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (fps != null && (fps == 15 || fps == 24 || fps == 30 || fps == 60)) {
                        onFpsChange(fps)
                        sendJsonResponse(out, """{"success": true, "fps": $fps}""")
                    } else {
                        sendJsonResponse(out, """{"error": "Invalid fps (supported: 15, 24, 30, 60)"}""", 400)
                    }
                    socket.close()
                }

                // CORS Preflight
                method == "OPTIONS" -> {
                    val corsHeader = "HTTP/1.1 204 No Content\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: Content-Type\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(corsHeader.toByteArray())
                    out.flush()
                    socket.close()
                }

                else -> {
                    val notFound = "Not Found".toByteArray()
                    val response = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: ${notFound.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    out.write(response.toByteArray())
                    out.write(notFound)
                    out.flush()
                    socket.close()
                }
            }
        } catch (e: Exception) {
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun sendJsonResponse(out: OutputStream, json: String, statusCode: Int = 200) {
        val statusText = if (statusCode == 200) "200 OK" else "$statusCode Error"
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusText\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Headers: Content-Type\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray())
        out.write(bytes)
        out.flush()
    }
}
