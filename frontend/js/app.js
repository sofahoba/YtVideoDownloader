// ============================================================
// app.js — Main application logic & event handling
// ============================================================

// -------------------------------------------------------
// Progress simulation (since yt-dlp doesn't stream progress)
// -------------------------------------------------------
let progressInterval = null;

function startProgressSimulation() {
    let current = 0;
    const stages = [
        { max: 20,  label: '🔍 Fetching video info...' },
        { max: 50,  label: '⬇ Downloading...' },
        { max: 75,  label: '🔄 Processing format...' },
        { max: 90,  label: '📦 Finalizing file...' },
    ];

    UI.showProgress('Starting...');

    progressInterval = setInterval(() => {
        current += Math.random() * 6;
        current  = Math.min(current, 90);

        const stage = stages.find(s => current <= s.max) || stages.at(-1);
        UI.updateProgress(Math.floor(current), stage.label);
    }, 700);
}

function stopProgressSimulation(success = true) {
    clearInterval(progressInterval);
    progressInterval = null;

    if (success) {
        UI.updateProgress(100, '✅ Done!');
        setTimeout(() => UI.hideProgress(), 1500);
    } else {
        UI.hideProgress();
    }
}

// -------------------------------------------------------
// URL Validation
// -------------------------------------------------------
function isValidYouTubeUrl(url) {
    const pattern = /^(https?:\/\/)?(www\.)?(youtube\.com\/(watch\?v=|shorts\/|embed\/)|youtu\.be\/).+/;
    return pattern.test(url);
}

// -------------------------------------------------------
// Handle Fetch Info Button
// -------------------------------------------------------
async function handleFetch() {
    const url = UI.getUrl();

    // Client-side validation
    UI.clearUrlError();
    if (!url) {
        UI.showUrlError('Please enter a YouTube URL');
        return;
    }
    if (!isValidYouTubeUrl(url)) {
        UI.showUrlError('Please enter a valid YouTube URL');
        return;
    }

    UI.setFetchLoading(true);
    UI.hideVideoInfo();

    try {
        const info = await Api.getVideoInfo(url);
        UI.showVideoInfo(info);

    } catch (error) {
        UI.showToast(`❌ ${error.message}`, 'error');
    } finally {
        UI.setFetchLoading(false);
    }
}

// -------------------------------------------------------
// Handle Download Button
// -------------------------------------------------------
async function handleDownload() {
    const url     = UI.getUrl();
    const format  = UI.getFormat();
    const quality = UI.getQuality();

    if (!url) {
        UI.showToast('No URL provided', 'error');
        return;
    }

    UI.setDownloadLoading(true);
    startProgressSimulation();

    try {
        const { blob, fileName } = await Api.downloadVideo(url, format, quality);

        // Trigger browser file save dialog
        triggerDownload(blob, fileName);

        stopProgressSimulation(true);
        UI.showToast(`✅ "${fileName}" saved!`, 'success', 5000);

    } catch (error) {
        stopProgressSimulation(false);
        UI.showToast(`❌ ${error.message}`, 'error');
    } finally {
        UI.setDownloadLoading(false);
    }
}

// -------------------------------------------------------
// Trigger browser download from Blob
// -------------------------------------------------------
function triggerDownload(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const a   = document.createElement('a');

    a.href     = url;
    a.download = fileName;
    a.style.display = 'none';

    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    // Revoke after short delay to ensure download starts
    setTimeout(() => URL.revokeObjectURL(url), 5000);
}

// -------------------------------------------------------
// Format Select Change
// -------------------------------------------------------
function onFormatChange() {
    const format = UI.getFormat();
    UI.toggleQualityGroup(format !== 'mp3');
}

// -------------------------------------------------------
// Enter key to trigger fetch
// -------------------------------------------------------
document.getElementById('urlInput').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') handleFetch();
});

// -------------------------------------------------------
// On page load: check API health
// -------------------------------------------------------
window.addEventListener('load', async () => {
    const isUp = await Api.checkHealth();
    if (!isUp) {
        UI.showToast('⚠️ Cannot reach the API. Is the backend running?', 'error', 6000);
    }
});