// Global App Utilities for ShortLink Pro
const App = {
    // Theme Management
    initTheme() {
        const savedTheme = localStorage.getItem('theme') || 'light';
        document.documentElement.setAttribute('data-theme', savedTheme);
        this.updateThemeToggleIcon(savedTheme);
        
        // Listen to toggle events
        const toggleBtn = document.getElementById('theme-toggle-btn');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                const currentTheme = document.documentElement.getAttribute('data-theme');
                const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
                document.documentElement.setAttribute('data-theme', newTheme);
                localStorage.setItem('theme', newTheme);
                this.updateThemeToggleIcon(newTheme);
            });
        }
    },
    
    updateThemeToggleIcon(theme) {
        const icon = document.querySelector('#theme-toggle-btn i');
        if (icon) {
            if (theme === 'dark') {
                icon.className = 'fas fa-sun';
            } else {
                icon.className = 'fas fa-moon';
            }
        }
    },

    // Elegant Toast Notifications
    showToast(message, type = 'success') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'position-fixed bottom-0 end-0 p-3';
            container.style.zIndex = '1090';
            document.body.appendChild(container);
        }

        const toastId = 'toast-' + Date.now();
        const icon = type === 'success' ? 'fa-check-circle text-success' : 
                     type === 'danger' ? 'fa-times-circle text-danger' : 'fa-info-circle text-info';
        
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center border-0 shadow-lg" role="alert" aria-live="assertive" aria-atomic="true" style="background: var(--bg-secondary); border-radius: 12px;">
                <div class="d-flex p-3">
                    <span class="me-2"><i class="fas ${icon}"></i></span>
                    <div class="toast-body p-0 text-muted" style="color: var(--text-primary) !important; font-weight: 500;">
                        ${message}
                    </div>
                    <button type="button" class="btn-close ms-auto me-0" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;
        
        container.insertAdjacentHTML('beforeend', toastHtml);
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { delay: 4000 });
        toast.show();
        
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    },

    // Copy to Clipboard Utility
    copyToClipboard(text, buttonElement) {
        navigator.clipboard.writeText(text).then(() => {
            this.showToast('Copied to clipboard!');
            if (buttonElement) {
                const originalHtml = buttonElement.innerHTML;
                buttonElement.innerHTML = '<i class="fas fa-check"></i>';
                buttonElement.classList.add('pop-active');
                
                setTimeout(() => {
                    buttonElement.innerHTML = originalHtml;
                    buttonElement.classList.remove('pop-active');
                }, 1500);
            }
        }).catch(err => {
            this.showToast('Failed to copy link.', 'danger');
            console.error('Clipboard copy error:', err);
        });
    },

    // Check Authentication Status
    async checkAuth() {
        try {
            const res = await fetch('/api/auth/status');
            if (res.ok) {
                return await res.json();
            }
        } catch (e) {
            console.error('Auth verification failed:', e);
        }
        return { authenticated: false };
    },

    // Logout Action
    async logout() {
        try {
            const res = await fetch('/api/auth/logout', { method: 'POST' });
            if (res.ok) {
                this.showToast('Successfully logged out!');
                setTimeout(() => {
                    window.location.href = '/login.html';
                }, 1000);
            } else {
                this.showToast('Logout failed.', 'danger');
            }
        } catch (e) {
            this.showToast('Connection error during logout.', 'danger');
        }
    },

    // Request Helper with automatic 401 handling
    async fetchApi(url, options = {}) {
        options.headers = options.headers || {};
        if (!(options.body instanceof FormData) && !options.headers['Content-Type']) {
            options.headers['Content-Type'] = 'application/json';
        }

        try {
            const response = await fetch(url, options);
            if (response.status === 401) {
                // Redirect to login if unauthorized and visiting a secure dashboard page
                if (window.location.pathname.includes('dashboard.html')) {
                    window.location.href = '/login.html?session_expired=true';
                }
            }
            return response;
        } catch (error) {
            console.error('API fetch error:', error);
            throw error;
        }
    }
};

// Auto-run on Page Load
document.addEventListener('DOMContentLoaded', () => {
    App.initTheme();
    
    // Auto setup logout button listeners
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            App.logout();
        });
    }
});
