// ============================================================
// api.js — All HTTP calls to the Spring Boot REST API
// ============================================================

const BASE_URL = 'http://192.168.1.7:8080/api/v1';

const Api = {

    /**
     * Fetch video metadata
     * GET /api/v1/info?url=...
     * @returns {Promise<{title, duration, thumbnail, uploader, views}>}
     */
    async getVideoInfo(url) {
        const response = await fetch(
            `${BASE_URL}/info?url=${encodeURIComponent(url)}`,
            {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            }
        );

        const body = await response.json();

        if (!response.ok || !body.success) {
            throw new Error(body.message || 'Failed to fetch video info');
        }

        return body.data;
    },

    /**
     * Download a video/audio file
     * POST /api/v1/download
     * @returns {Promise<{blob: Blob, fileName: string}>}
     */
    async downloadVideo(url, format, quality) {
        const response = await fetch(`${BASE_URL}/download`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': '*/*'
            },
            body: JSON.stringify({ url, format, quality })
        });

        // If error, parse JSON error body
        if (!response.ok) {
            const errorBody = await response.json();
            throw new Error(errorBody.message || 'Download failed');
        }

        // Extract filename from Content-Disposition header
        const disposition = response.headers.get('Content-Disposition') || '';
        const fileName = parseFileName(disposition) || `download.${format}`;

        const blob = await response.blob();
        return { blob, fileName };
    },

    /**
     * Health check
     * GET /api/v1/health
     */
    async checkHealth() {
        const response = await fetch(`${BASE_URL}/health`);
        return response.ok;
    }
};

// -------------------------------------------------------
// Helper: extract filename from Content-Disposition header
// -------------------------------------------------------
function parseFileName(disposition) {
    // Try filename*=UTF-8''...
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;\n]+)/i);
    if (utf8Match) return decodeURIComponent(utf8Match[1]);

    // Try filename="..."
    const basicMatch = disposition.match(/filename="?([^";\n]+)"?/i);
    if (basicMatch) return basicMatch[1].trim();

    return null;
}