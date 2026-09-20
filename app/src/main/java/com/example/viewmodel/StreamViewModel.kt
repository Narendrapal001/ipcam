package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ResolutionPreset
import com.example.model.StreamQualitySettings
import com.example.model.StreamServerState
import com.example.network.NetworkUtils
import com.example.server.MjpegHttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StreamViewModel(application: Application) : AndroidViewModel(application) {

    private val _settings = MutableStateFlow(StreamQualitySettings())
    val settings: StateFlow<StreamQualitySettings> = _settings.asStateFlow()

    private val _serverState = MutableStateFlow(
        StreamServerState(
            ipAddress = NetworkUtils.getLocalIpAddress(),
            port = 8080
        )
    )
    val serverState: StateFlow<StreamServerState> = _serverState.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private var httpServer: MjpegHttpServer? = null

    // Callbacks for CameraStreamManager
    var onCameraTorchToggled: ((Boolean) -> Unit)? = null
    var onCameraFlipped: (() -> Unit)? = null
    var onCameraSettingsChanged: ((ResolutionPreset, Int, Int, Boolean) -> Unit)? = null

    init {
        // Automatically start the streaming server on launch so user gets their URL instantly!
        startServer()

        // Periodic ticker to refresh stats and IP
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                updateServerMetrics()
            }
        }
    }

    fun startServer() {
        if (_serverState.value.isStreaming) return

        val currentPort = _settings.value.port
        val currentIp = NetworkUtils.getLocalIpAddress()

        httpServer = MjpegHttpServer(
            port = currentPort,
            getSettings = { _settings.value },
            getLocalIp = { NetworkUtils.getLocalIpAddress() },
            onToggleTorch = { enabled ->
                viewModelScope.launch(Dispatchers.Main) {
                    setTorch(enabled)
                }
            },
            onFlipCamera = {
                viewModelScope.launch(Dispatchers.Main) {
                    switchCamera()
                }
            },
            onQualityChange = { preset ->
                viewModelScope.launch(Dispatchers.Main) {
                    setResolution(preset)
                }
            },
            onFpsChange = { fps ->
                viewModelScope.launch(Dispatchers.Main) {
                    setFps(fps)
                }
            },
            onClientCountChanged = { count ->
                _serverState.update { it.copy(connectedClients = count) }
            }
        ).also {
            it.start()
        }

        _serverState.update {
            it.copy(
                isStreaming = true,
                ipAddress = currentIp,
                port = currentPort,
                lastError = null
            )
        }
    }

    fun stopServer() {
        httpServer?.stop()
        httpServer = null
        _serverState.update {
            it.copy(
                isStreaming = false,
                connectedClients = 0,
                currentFps = 0.0,
                bitrateKbps = 0.0
            )
        }
    }

    fun toggleStreaming() {
        if (_serverState.value.isStreaming) {
            stopServer()
        } else {
            startServer()
        }
    }

    fun broadcastFrame(jpegBytes: ByteArray) {
        httpServer?.broadcastFrame(jpegBytes)
    }

    fun setResolution(preset: ResolutionPreset) {
        _settings.update { it.copy(resolution = preset) }
        notifyCameraSettingsChanged()
    }

    fun setFps(fps: Int) {
        _settings.update { it.copy(targetFps = fps) }
        notifyCameraSettingsChanged()
    }

    fun setJpegQuality(quality: Int) {
        _settings.update { it.copy(jpegQuality = quality) }
        notifyCameraSettingsChanged()
    }

    fun setPort(port: Int) {
        if (port in 1024..65535 && port != _settings.value.port) {
            val wasStreaming = _serverState.value.isStreaming
            if (wasStreaming) stopServer()
            _settings.update { it.copy(port = port) }
            if (wasStreaming) startServer()
        }
    }

    fun setTorch(enabled: Boolean) {
        if (!_settings.value.isBackCamera && enabled) return
        _settings.update { it.copy(isTorchOn = enabled) }
        onCameraTorchToggled?.invoke(enabled)
    }

    fun toggleTorch() {
        setTorch(!_settings.value.isTorchOn)
    }

    fun switchCamera() {
        val newFacing = !_settings.value.isBackCamera
        _settings.update {
            it.copy(
                isBackCamera = newFacing,
                isTorchOn = false // reset torch when switching
            )
        }
        onCameraFlipped?.invoke()
        notifyCameraSettingsChanged()
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    private fun notifyCameraSettingsChanged() {
        val s = _settings.value
        onCameraSettingsChanged?.invoke(s.resolution, s.targetFps, s.jpegQuality, s.isBackCamera)
    }

    private fun updateServerMetrics() {
        val server = httpServer
        val ip = NetworkUtils.getLocalIpAddress()
        if (server != null) {
            _serverState.update {
                it.copy(
                    ipAddress = ip,
                    currentFps = server.currentFps,
                    bitrateKbps = server.currentBitrateKbps
                )
            }
        } else {
            _serverState.update { it.copy(ipAddress = ip) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpServer?.stop()
    }
}
