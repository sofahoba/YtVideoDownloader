// ============================================================
// ui.js — All DOM manipulation and UI state management
// ============================================================

const UI = {

    // -------------------------------------------------------
    // Elements (cached for performance)
    // -------------------------------------------------------
    els: {
        urlInput:      () => document.getElementById('urlInput'),
        urlError:      () => document.getElementById('urlError'),
        fetchBtn:      () => document.getElementById('fetchBtn'),
        videoInfoCard: () => document.getElementById('videoInfoCard'),
        thumbnail:     () => document.getElementById('thumbnail'),
        videoTitle:    () => document.getElementById('videoTitle'),
        videoUploader: () => document.getElementById('videoUploader'),
        videoDuration: () => document.getElementById('videoDuration'),
        videoViews:    () => document.getElementById('videoViews'),
        formatSelect:  () => document.getElementById('formatSelect'),
        qualitySelect: () => document.getElementById('qualitySelect'),
        qualityGroup:  () => document.getElementById('qualityGroup'),
        downloadBtn:   () => document.getElementById('downloadBtn'),
        progressCard:  () => document.getElementById('progressCard'),
        progressLabel: () => document.getElementById('progressLabel'),
        progressFill:  () => document.getElementById('progressFill'),
        toast:         () => document.getElementById('toast'),
    },

    // -------------------------------------------------------
    // Video Info
    // -------------------------------------------------------
    showVideoInfo(info) {
        this.els.thumbnail().src              = info.thumbnail;
        this.els.videoTitle().textContent     = info.title;
        this.els.videoUploader().textContent  = `👤 ${info.uploader}`;
        this.els.videoDuration().textContent  = `⏱ ${info.duration}`;
        this.els.videoViews().textContent     = `👁 ${info.views} views`;

        this.show('videoInfoCard');
    },

    hideVideoInfo() {
        this.hide('videoInfoCard');
    },

    // -------------------------------------------------------
    // Fetch Button State
    // -------------------------------------------------------
    setFetchLoading(isLoading) {
        const btn = this.els.fetchBtn();
        btn.disabled    = isLoading;
        btn.textContent = isLoading ? 'Fetching...' : 'Fetch Info';
    },

    // -------------------------------------------------------
    // Download Button State
    // -------------------------------------------------------
    setDownloadLoading(isLoading) {
        const btn = this.els.downloadBtn();
        btn.disabled    = isLoading;
        btn.textContent = isLoading ? '⏳ Downloading...' : '⬇ Download';
    },

    // -------------------------------------------------------
    // Progress Bar
    // -------------------------------------------------------
    showProgress(label = 'Preparing...') {
        this.els.progressLabel().textContent  = label;
        this.els.progressFill().style.width   = '0%';
        this.show('progressCard');
    },

    updateProgress(percent, label) {
        this.els.progressFill().style.width = `${Math.min(percent, 100)}%`;
        if (label) this.els.progressLabel().textContent = label;
    },

    hideProgress() {
        this.hide('progressCard');
        this.els.progressFill().style.width = '0%';
    },

    // -------------------------------------------------------
    // URL Validation Error
    // -------------------------------------------------------
    showUrlError(msg) {
        const el = this.els.urlError();
        el.textContent = msg;
        this.show('urlError');
    },

    clearUrlError() {
        this.hide('urlError');
        this.els.urlError().textContent = '';
    },

    // -------------------------------------------------------
    // Quality Group Toggle (hidden for MP3)
    // -------------------------------------------------------
    toggleQualityGroup(show) {
        const group = this.els.qualityGroup();
        if (show) {
            group.classList.remove('disabled-group');
        } else {
            group.classList.add('disabled-group');
        }
    },

    // -------------------------------------------------------
    // Toast Notification
    // -------------------------------------------------------
    _toastTimer: null,

    showToast(message, type = 'info', duration = 4000) {
        const toast = this.els.toast();
        toast.textContent = message;
        toast.className   = `toast ${type}`;

        // Show
        requestAnimationFrame(() => toast.classList.add('show'));

        // Auto hide
        clearTimeout(this._toastTimer);
        this._toastTimer = setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.classList.add('hidden'), 350);
        }, duration);
    },

    // -------------------------------------------------------
    // Utility
    // -------------------------------------------------------
    show(id) { document.getElementById(id)?.classList.remove('hidden'); },
    hide(id) { document.getElementById(id)?.classList.add('hidden');    },

    getUrl()     { return this.els.urlInput().value.trim(); },
    getFormat()  { return this.els.formatSelect().value; },
    getQuality() { return this.els.qualitySelect().value; },
};