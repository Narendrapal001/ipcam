package com.example.model

enum class ResolutionPreset(
    val title: String,
    val subtitle: String,
    val width: Int,
    val height: Int
) {
    FHD_1080P("1080p", "Full HD (1920×1080)", 1920, 1080),
    HD_720P("720p", "High Def (1280×720)", 1280, 720),
    SD_480P("480p", "Standard (640×480)", 640, 480),
    LD_360P("360p", "Low Latency (480×360)", 480, 360);

    val label: String
        get() = "$title ($width×$height)"
}

data class StreamQualitySettings(
    val resolution: ResolutionPreset = ResolutionPreset.HD_720P,
    val targetFps: Int = 30,
    val jpegQuality: Int = 65,
    val port: Int = 8080,
    val isBackCamera: Boolean = true,
    val isTorchOn: Boolean = false
)

data class StreamServerState(
    val isStreaming: Boolean = false,
    val ipAddress: String = "0.0.0.0",
    val port: Int = 8080,
    val connectedClients: Int = 0,
    val currentFps: Double = 0.0,
    val bitrateKbps: Double = 0.0,
    val totalFramesSent: Long = 0L,
    val lastError: String? = null
) {
    val streamUrl: String
        get() = "http://$ipAddress:$port"

    val videoFeedUrl: String
        get() = "$streamUrl/stream"
}
