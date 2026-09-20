package com.example.server

import com.example.model.StreamQualitySettings

object WebClientHtml {

    fun generateHtml(settings: StreamQualitySettings, ip: String, port: Int): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CamStream — Real-Time Phone Camera</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-primary: #090d16;
            --glass-bg: rgba(18, 24, 43, 0.65);
            --glass-card: rgba(26, 35, 62, 0.45);
            --glass-border: rgba(255, 255, 255, 0.12);
            --glass-border-hover: rgba(56, 189, 248, 0.4);
            --neon-cyan: #38bdf8;
            --neon-violet: #818cf8;
            --neon-emerald: #34d399;
            --neon-rose: #fb7185;
            --text-primary: #f8fafc;
            --text-secondary: #94a3b8;
            --text-muted: #64748b;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: var(--bg-primary);
            color: var(--text-primary);
            min-height: 100vh;
            overflow-x: hidden;
            display: flex;
            flex-direction: column;
            background-image: 
                radial-gradient(circle at 15% 15%, rgba(56, 189, 248, 0.15) 0%, transparent 45%),
                radial-gradient(circle at 85% 85%, rgba(129, 140, 248, 0.15) 0%, transparent 45%),
                radial-gradient(circle at 50% 50%, rgba(9, 13, 22, 0.95) 0%, rgba(9, 13, 22, 1) 100%);
            background-attachment: fixed;
        }

        /* Glassmorphism Header */
        header {
            position: sticky;
            top: 0;
            z-index: 50;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 16px 28px;
            background: var(--glass-bg);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border-bottom: 1px solid var(--glass-border);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
        }

        .brand-container {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .brand-icon {
            width: 40px;
            height: 40px;
            border-radius: 12px;
            background: linear-gradient(135deg, rgba(56, 189, 248, 0.25), rgba(129, 140, 248, 0.25));
            border: 1px solid rgba(255, 255, 255, 0.2);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 15px rgba(56, 189, 248, 0.2);
        }

        .brand-icon svg {
            width: 22px;
            height: 22px;
            fill: var(--neon-cyan);
        }

        .brand-title {
            font-size: 1.25rem;
            font-weight: 700;
            letter-spacing: -0.02em;
            background: linear-gradient(135deg, #ffffff 30%, var(--neon-cyan) 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .brand-badge {
            font-size: 0.7rem;
            font-weight: 600;
            text-transform: uppercase;
            padding: 2px 8px;
            border-radius: 9999px;
            background: rgba(56, 189, 248, 0.15);
            color: var(--neon-cyan);
            border: 1px solid rgba(56, 189, 248, 0.3);
            margin-left: 6px;
            letter-spacing: 0.05em;
        }

        /* Status & Stats bar */
        .status-strip {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .live-pill {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 14px;
            border-radius: 9999px;
            background: rgba(52, 211, 153, 0.12);
            border: 1px solid rgba(52, 211, 153, 0.3);
            font-size: 0.78rem;
            font-weight: 600;
            color: var(--neon-emerald);
        }

        .live-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: var(--neon-emerald);
            box-shadow: 0 0 10px var(--neon-emerald);
            animation: pulse-dot 1.8s infinite ease-in-out;
        }

        @keyframes pulse-dot {
            0%, 100% { transform: scale(1); opacity: 1; }
            50% { transform: scale(1.4); opacity: 0.6; }
        }

        .stat-chip {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 6px 12px;
            border-radius: 8px;
            background: var(--glass-card);
            border: 1px solid var(--glass-border);
            font-size: 0.78rem;
            font-family: 'JetBrains Mono', monospace;
            color: var(--text-secondary);
        }

        .stat-chip strong {
            color: var(--text-primary);
        }

        /* Main Viewport */
        main {
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 24px 28px 48px;
            max-width: 1400px;
            width: 100%;
            margin: 0 auto;
        }

        .stage-container {
            width: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 20px;
        }

        /* Video Feed Glass Frame */
        .video-viewport {
            position: relative;
            width: 100%;
            max-width: 1040px;
            aspect-ratio: 16 / 9;
            border-radius: 20px;
            overflow: hidden;
            background: rgba(12, 16, 28, 0.8);
            border: 1px solid var(--glass-border);
            box-shadow: 
                0 20px 50px rgba(0, 0, 0, 0.6),
                0 0 40px rgba(56, 189, 248, 0.08);
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .video-viewport:hover {
            border-color: var(--glass-border-hover);
            box-shadow: 
                0 25px 60px rgba(0, 0, 0, 0.7),
                0 0 50px rgba(56, 189, 248, 0.15);
        }

        .video-feed {
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: block;
            user-select: none;
            transition: transform 0.2s ease;
        }

        .video-feed.mirrored {
            transform: scaleX(-1);
        }

        /* Viewfinder overlay */
        .viewfinder-corner {
            position: absolute;
            width: 24px;
            height: 24px;
            border-color: rgba(56, 189, 248, 0.6);
            pointer-events: none;
            transition: all 0.3s ease;
        }

        .corner-tl { top: 18px; left: 18px; border-top: 2px solid; border-left: 2px solid; border-top-left-radius: 6px; }
        .corner-tr { top: 18px; right: 18px; border-top: 2px solid; border-right: 2px solid; border-top-right-radius: 6px; }
        .corner-bl { bottom: 18px; left: 18px; border-bottom: 2px solid; border-left: 2px solid; border-bottom-left-radius: 6px; }
        .corner-br { bottom: 18px; right: 18px; border-bottom: 2px solid; border-right: 2px solid; border-bottom-right-radius: 6px; }

        .overlay-hud {
            position: absolute;
            top: 20px;
            left: 20px;
            display: flex;
            gap: 10px;
            pointer-events: none;
        }

        .hud-badge {
            background: rgba(9, 13, 22, 0.75);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 4px 10px;
            border-radius: 6px;
            font-size: 0.72rem;
            font-family: 'JetBrains Mono', monospace;
            color: var(--neon-cyan);
            letter-spacing: 0.05em;
        }

        /* Floating Glass Dock Controls */
        .glass-dock {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 10px 18px;
            background: var(--glass-bg);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid var(--glass-border);
            border-radius: 20px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
            flex-wrap: wrap;
            justify-content: center;
        }

        .control-group {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .divider {
            width: 1px;
            height: 28px;
            background: var(--glass-border);
            margin: 0 4px;
        }

        .dock-btn {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 9px 16px;
            border-radius: 12px;
            background: var(--glass-card);
            border: 1px solid var(--glass-border);
            color: var(--text-primary);
            font-family: inherit;
            font-size: 0.85rem;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            user-select: none;
        }

        .dock-btn:hover {
            background: rgba(56, 189, 248, 0.15);
            border-color: rgba(56, 189, 248, 0.4);
            color: #ffffff;
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(56, 189, 248, 0.2);
        }

        .dock-btn:active {
            transform: translateY(0);
        }

        .dock-btn.active {
            background: rgba(56, 189, 248, 0.25);
            border-color: var(--neon-cyan);
            color: #ffffff;
            box-shadow: 0 0 16px rgba(56, 189, 248, 0.3);
        }

        .dock-btn svg {
            width: 18px;
            height: 18px;
            fill: currentColor;
        }

        .select-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        .select-label {
            font-size: 0.75rem;
            font-weight: 600;
            color: var(--text-muted);
            margin-right: 8px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        select.glass-select {
            appearance: none;
            background: var(--glass-card);
            border: 1px solid var(--glass-border);
            color: var(--text-primary);
            padding: 8px 32px 8px 12px;
            border-radius: 10px;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.82rem;
            cursor: pointer;
            outline: none;
            transition: all 0.2s ease;
        }

        select.glass-select:hover, select.glass-select:focus {
            border-color: var(--neon-cyan);
            background: rgba(26, 35, 62, 0.7);
        }

        select.glass-select option {
            background: #0d1322;
            color: #ffffff;
        }

        .select-wrapper::after {
            content: '▾';
            position: absolute;
            right: 12px;
            color: var(--text-secondary);
            pointer-events: none;
            font-size: 0.8rem;
        }

        /* Toast notification */
        .toast {
            position: fixed;
            bottom: 24px;
            left: 50%;
            transform: translateX(-50%) translateY(100px);
            background: rgba(18, 24, 43, 0.95);
            backdrop-filter: blur(20px);
            border: 1px solid var(--neon-cyan);
            color: #ffffff;
            padding: 12px 24px;
            border-radius: 14px;
            font-size: 0.88rem;
            font-weight: 500;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 20px rgba(56, 189, 248, 0.3);
            display: flex;
            align-items: center;
            gap: 10px;
            z-index: 100;
            opacity: 0;
            transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
            pointer-events: none;
        }

        .toast.show {
            transform: translateX(-50%) translateY(0);
            opacity: 1;
        }

        /* Keyboard shortcuts tip */
        .shortcut-hint {
            font-size: 0.78rem;
            color: var(--text-muted);
            margin-top: 8px;
            display: flex;
            gap: 16px;
            flex-wrap: wrap;
            justify-content: center;
        }

        .kbd {
            background: rgba(255, 255, 255, 0.08);
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 4px;
            padding: 1px 6px;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.72rem;
            color: var(--text-secondary);
        }

        /* Error state */
        .error-banner {
            display: none;
            background: rgba(251, 113, 133, 0.15);
            border: 1px solid rgba(251, 113, 133, 0.4);
            color: var(--neon-rose);
            padding: 10px 20px;
            border-radius: 12px;
            font-size: 0.85rem;
            margin-bottom: 16px;
            align-items: center;
            gap: 8px;
        }
    </style>
</head>
<body>
    <header>
        <div class="brand-container">
            <div class="brand-icon">
                <svg viewBox="0 0 24 24">
                    <path d="M4 4h3l2-2h6l2 2h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm8 3a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0 2a3 3 0 1 1 0 6 3 3 0 0 1 0-6z"/>
                </svg>
            </div>
            <div>
                <span class="brand-title">CamStream</span>
                <span class="brand-badge">Wireless HD</span>
            </div>
        </div>

        <div class="status-strip">
            <div class="live-pill" id="livePill">
                <span class="live-dot"></span>
                <span>LIVE FEED</span>
            </div>
            <div class="stat-chip">
                <span>FPS:</span>
                <strong id="fpsVal">--</strong>
            </div>
            <div class="stat-chip">
                <span>RES:</span>
                <strong id="resVal">${settings.resolution.title}</strong>
            </div>
            <div class="stat-chip">
                <span>LATENCY:</span>
                <strong id="pingVal">&lt;50ms</strong>
            </div>
        </div>
    </header>

    <main>
        <div id="errorBanner" class="error-banner">
            <span>⚠️ Stream disconnected. Attempting to reconnect...</span>
        </div>

        <div class="stage-container">
            <div class="video-viewport" id="viewport">
                <img id="videoStream" class="video-feed" src="/stream" alt="Live Camera Stream" />
                
                <div class="viewfinder-corner corner-tl"></div>
                <div class="viewfinder-corner corner-tr"></div>
                <div class="viewfinder-corner corner-bl"></div>
                <div class="viewfinder-corner corner-br"></div>

                <div class="overlay-hud">
                    <div class="hud-badge" id="cameraFacingBadge">BACK CAMERA</div>
                    <div class="hud-badge" id="timestampBadge">00:00:00</div>
                </div>
            </div>

            <!-- Floating Glass Dock -->
            <div class="glass-dock">
                <!-- Action Buttons -->
                <div class="control-group">
                    <button class="dock-btn" id="snapshotBtn" title="Take high-resolution photo (S)">
                        <svg viewBox="0 0 24 24"><path d="M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6zm-7 10h14a1 1 0 0 0 1-1V8a1 1 0 0 0-1-1h-3.17l-1.42-1.41A1 1 0 0 0 13.71 5H10.3a1 1 0 0 0-.71.29L8.17 7H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1z"/></svg>
                        <span>Snapshot</span>
                    </button>

                    <button class="dock-btn" id="torchBtn" title="Toggle Flashlight on phone (T)">
                        <svg viewBox="0 0 24 24"><path d="M7 2v11h3v9l7-12h-4l4-8z"/></svg>
                        <span>Torch</span>
                    </button>

                    <button class="dock-btn" id="flipBtn" title="Switch between front and back cameras (C)">
                        <svg viewBox="0 0 24 24"><path d="M20 5h-3.17L15 3H9L7.17 5H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-8 13c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm3-5c0 1.66-1.34 3-3 3s-3-1.34-3-3 1.34-3 3-3 3 1.34 3 3z"/></svg>
                        <span>Flip Camera</span>
                    </button>

                    <button class="dock-btn" id="mirrorBtn" title="Mirror video horizontally">
                        <svg viewBox="0 0 24 24"><path d="M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"/></svg>
                        <span>Mirror</span>
                    </button>
                </div>

                <div class="divider"></div>

                <!-- Quality Dropdown -->
                <div class="control-group">
                    <span class="select-label">Quality</span>
                    <div class="select-wrapper">
                        <select id="qualitySelect" class="glass-select">
                            <option value="FHD_1080P" ${if (settings.resolution.name == "FHD_1080P") "selected" else ""}>1080p FHD</option>
                            <option value="HD_720P" ${if (settings.resolution.name == "HD_720P") "selected" else ""}>720p HD</option>
                            <option value="SD_480P" ${if (settings.resolution.name == "SD_480P") "selected" else ""}>480p SD</option>
                            <option value="LD_360P" ${if (settings.resolution.name == "LD_360P") "selected" else ""}>360p Low Latency</option>
                        </select>
                    </div>
                </div>

                <!-- FPS Dropdown -->
                <div class="control-group">
                    <span class="select-label">FPS</span>
                    <div class="select-wrapper">
                        <select id="fpsSelect" class="glass-select">
                            <option value="60" ${if (settings.targetFps == 60) "selected" else ""}>60 FPS</option>
                            <option value="30" ${if (settings.targetFps == 30) "selected" else ""}>30 FPS</option>
                            <option value="24" ${if (settings.targetFps == 24) "selected" else ""}>24 FPS</option>
                            <option value="15" ${if (settings.targetFps == 15) "selected" else ""}>15 FPS</option>
                        </select>
                    </div>
                </div>

                <div class="divider"></div>

                <!-- Fullscreen & Reload -->
                <div class="control-group">
                    <button class="dock-btn" id="fullscreenBtn" title="Toggle Fullscreen (F)">
                        <svg viewBox="0 0 24 24"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/></svg>
                        <span>Fullscreen</span>
                    </button>
                </div>
            </div>

            <div class="shortcut-hint">
                <span>Shortcuts: <span class="kbd">S</span> Snapshot</span>
                <span><span class="kbd">T</span> Torch</span>
                <span><span class="kbd">C</span> Flip</span>
                <span><span class="kbd">M</span> Mirror</span>
                <span><span class="kbd">F</span> Fullscreen</span>
            </div>
        </div>
    </main>

    <div id="toast" class="toast">
        <span id="toastMsg">Action executed</span>
    </div>

    <script>
        const videoStream = document.getElementById('videoStream');
        const viewport = document.getElementById('viewport');
        const livePill = document.getElementById('livePill');
        const fpsVal = document.getElementById('fpsVal');
        const resVal = document.getElementById('resVal');
        const pingVal = document.getElementById('pingVal');
        const errorBanner = document.getElementById('errorBanner');
        const toast = document.getElementById('toast');
        const toastMsg = document.getElementById('toastMsg');
        const qualitySelect = document.getElementById('qualitySelect');
        const fpsSelect = document.getElementById('fpsSelect');
        const torchBtn = document.getElementById('torchBtn');
        const flipBtn = document.getElementById('flipBtn');
        const mirrorBtn = document.getElementById('mirrorBtn');
        const snapshotBtn = document.getElementById('snapshotBtn');
        const fullscreenBtn = document.getElementById('fullscreenBtn');
        const cameraFacingBadge = document.getElementById('cameraFacingBadge');
        const timestampBadge = document.getElementById('timestampBadge');

        let isTorchOn = ${settings.isTorchOn};
        let isBackCamera = ${settings.isBackCamera};
        let isMirrored = false;

        function showToast(msg) {
            toastMsg.innerText = msg;
            toast.classList.add('show');
            setTimeout(() => toast.classList.remove('show'), 2500);
        }

        // Clock update
        setInterval(() => {
            const now = new Date();
            timestampBadge.innerText = now.toTimeString().split(' ')[0];
        }, 1000);

        // Periodic Status Poller
        async function fetchStatus() {
            try {
                const startTime = performance.now();
                const res = await fetch('/api/status');
                const elapsed = Math.round(performance.now() - startTime);
                if (res.ok) {
                    const data = await res.json();
                    errorBanner.style.display = 'none';
                    fpsVal.innerText = data.fps ? data.fps.toFixed(0) : '--';
                    resVal.innerText = data.resolutionTitle || '${settings.resolution.title}';
                    pingVal.innerText = elapsed + 'ms';
                    isTorchOn = data.torch;
                    isBackCamera = data.isBackCamera;
                    cameraFacingBadge.innerText = isBackCamera ? 'BACK CAMERA' : 'FRONT CAMERA';
                    torchBtn.classList.toggle('active', isTorchOn);
                }
            } catch (err) {
                errorBanner.style.display = 'flex';
            }
        }

        setInterval(fetchStatus, 2000);
        fetchStatus();

        // Reload Stream cleanly
        function reloadStream() {
            videoStream.src = '/stream?t=' + Date.now();
        }

        videoStream.onerror = () => {
            errorBanner.style.display = 'flex';
            setTimeout(reloadStream, 2000);
        };

        videoStream.onload = () => {
            errorBanner.style.display = 'none';
        };

        // Snapshot Button
        snapshotBtn.addEventListener('click', () => {
            const link = document.createElement('a');
            link.href = '/snapshot?t=' + Date.now();
            link.download = 'camstream_' + new Date().toISOString().replace(/[:.]/g, '-') + '.jpg';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            showToast('📸 Snapshot captured and saved!');
        });

        // Torch Toggle
        async function toggleTorch() {
            try {
                const nextState = !isTorchOn;
                const res = await fetch('/api/torch', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ enabled: nextState })
                });
                if (res.ok) {
                    isTorchOn = nextState;
                    torchBtn.classList.toggle('active', isTorchOn);
                    showToast(isTorchOn ? '🔦 Torch turned ON' : '🔦 Torch turned OFF');
                }
            } catch (e) {
                showToast('❌ Failed to toggle torch');
            }
        }
        torchBtn.addEventListener('click', toggleTorch);

        // Flip Camera
        async function flipCamera() {
            try {
                const res = await fetch('/api/flip', { method: 'POST' });
                if (res.ok) {
                    isBackCamera = !isBackCamera;
                    cameraFacingBadge.innerText = isBackCamera ? 'BACK CAMERA' : 'FRONT CAMERA';
                    showToast('🔄 Switched to ' + (isBackCamera ? 'Back Camera' : 'Front Camera'));
                    setTimeout(reloadStream, 500);
                }
            } catch (e) {
                showToast('❌ Failed to switch camera');
            }
        }
        flipBtn.addEventListener('click', flipCamera);

        // Mirror Toggle
        mirrorBtn.addEventListener('click', () => {
            isMirrored = !isMirrored;
            videoStream.classList.toggle('mirrored', isMirrored);
            mirrorBtn.classList.toggle('active', isMirrored);
            showToast(isMirrored ? '🪞 Video feed mirrored' : 'Video feed unmirrored');
        });

        // Quality Select Change
        qualitySelect.addEventListener('change', async (e) => {
            const preset = e.target.value;
            try {
                const res = await fetch('/api/quality', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ preset })
                });
                if (res.ok) {
                    showToast('✨ Quality changed to ' + e.target.options[e.target.selectedIndex].text);
                    setTimeout(reloadStream, 600);
                }
            } catch (err) {
                showToast('❌ Failed to change quality');
            }
        });

        // FPS Select Change
        fpsSelect.addEventListener('change', async (e) => {
            const fps = parseInt(e.target.value);
            try {
                const res = await fetch('/api/fps', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ fps })
                });
                if (res.ok) {
                    showToast('⚡ Target frame rate set to ' + fps + ' FPS');
                }
            } catch (err) {
                showToast('❌ Failed to change FPS');
            }
        });

        // Fullscreen Toggle
        fullscreenBtn.addEventListener('click', () => {
            if (!document.fullscreenElement) {
                viewport.requestFullscreen().catch(err => {
                    alert('Error enabling fullscreen: ' + err.message);
                });
            } else {
                document.exitFullscreen();
            }
        });

        // Keyboard Shortcuts
        window.addEventListener('keydown', (e) => {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT') return;
            const key = e.key.toLowerCase();
            if (key === 's') snapshotBtn.click();
            else if (key === 't') toggleTorch();
            else if (key === 'c') flipCamera();
            else if (key === 'm') mirrorBtn.click();
            else if (key === 'f') fullscreenBtn.click();
        });
    </script>
</body>
</html>
        """.trimIndent()
    }
}
