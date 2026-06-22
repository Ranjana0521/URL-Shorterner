// Landing Page Actions for ShortLink Pro
document.addEventListener('DOMContentLoaded', async () => {
    // 1. Check for URL redirect errors and show toasts
    const urlParams = new URLSearchParams(window.location.search);
    const error = urlParams.get('error');
    if (error) {
        if (error === 'not_found') {
            App.showToast('The short link you requested does not exist.', 'danger');
        } else if (error === 'expired') {
            App.showToast('This short link has expired and is no longer available.', 'danger');
        } else if (error === 'disabled') {
            App.showToast('This short link has been disabled by the owner.', 'danger');
        }
        // Clean URL params to avoid persistent error alerts on reload
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    // 2. Dynamically adjust navigation bar depending on user login state
    const authStatus = await App.checkAuth();
    const authContainer = document.getElementById('auth-buttons-container');
    if (authContainer && authStatus.authenticated) {
        authContainer.innerHTML = `
            <a class="btn btn-premium-outline me-2" href="/dashboard.html">
                <i class="fas fa-chart-pie me-1"></i>Dashboard
            </a>
            <button class="btn btn-premium" id="nav-logout-btn">
                <i class="fas fa-sign-out-alt me-1"></i>Logout
            </button>
        `;
        // Setup logout trigger
        document.getElementById('nav-logout-btn').addEventListener('click', (e) => {
            e.preventDefault();
            App.logout();
        });
    }

    // 3. Handle Guest URL Shortening Submission
    const shortenForm = document.getElementById('landing-shorten-form');
    const resultDisplay = document.getElementById('result-display');
    const shortenedUrlText = document.getElementById('shortened-url-text');
    const resultQrCode = document.getElementById('result-qr-code');
    const copyBtn = document.getElementById('copy-btn');

    if (shortenForm) {
        shortenForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const originalUrl = document.getElementById('originalUrl').value;

            try {
                const response = await App.fetchApi('/api/urls', {
                    method: 'POST',
                    body: JSON.stringify({ originalUrl: originalUrl })
                });

                if (response.ok) {
                    const data = await response.json();
                    
                    // Show result box
                    resultDisplay.style.display = 'block';
                    shortenedUrlText.value = data.shortUrl;
                    
                    // Bind QR Code Base64 PNG image
                    resultQrCode.src = `data:image/png;base64,${data.qrCode}`;
                    
                    App.showToast('Link shortened successfully!');

                    // Setup Copy Button
                    copyBtn.onclick = () => {
                        App.copyToClipboard(data.shortUrl, copyBtn);
                    };
                    
                    // Scroll to result view smoothly
                    resultDisplay.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                } else {
                    const errData = await response.json();
                    App.showToast(errData.error || 'Failed to shorten URL.', 'danger');
                }
            } catch (err) {
                App.showToast('Network error. Please try again.', 'danger');
                console.error(err);
            }
        });
    }
});
