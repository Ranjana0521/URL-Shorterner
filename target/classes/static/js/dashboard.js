// Dashboard JS Controller for ShortLink Pro
document.addEventListener('DOMContentLoaded', async () => {
    
    // Global State variables
    let currentUser = null;
    let currentView = 'overview-view';
    let charts = {}; // Holds Chart.js instances
    
    // Link Manager Page State
    let currentPage = 0;
    let pageSize = 7;
    let currentSearch = '';
    let currentSortBy = 'createdAt';
    let currentOrder = 'desc';

    // 1. Verify User Session on Load
    const authStatus = await App.checkAuth();
    if (!authStatus.authenticated) {
        window.location.href = '/login.html?session_expired=true';
        return;
    }
    
    currentUser = authStatus;
    document.getElementById('user-display-name').textContent = currentUser.username;
    const settingsEmail = document.getElementById('settings-email');
    if (settingsEmail) {
        settingsEmail.value = currentUser.email;
    }

    // 2. Navigation Tab Manager
    const sidebarLinks = document.querySelectorAll('.sidebar-link[data-view]');
    const views = document.querySelectorAll('.dashboard-view-panel');

    sidebarLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetView = link.getAttribute('data-view');
            
            // Remove active states
            sidebarLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            
            // Switch views
            views.forEach(v => v.style.display = 'none');
            const activePanel = document.getElementById(targetView);
            if (activePanel) {
                activePanel.style.display = 'block';
            }
            
            currentView = targetView;
            loadViewData(targetView);
        });
    });

    // 3. Centralized View Data Loader
    function loadViewData(viewId) {
        const loader = document.getElementById('view-loader');
        if (loader) loader.removeAttribute('style'); // Show loader
        
        try {
            switch(viewId) {
                case 'overview-view':
                    loadOverviewData();
                    break;
                case 'links-view':
                    loadLinksData();
                    break;
                case 'analytics-view':
                    loadAnalyticsData();
                    break;
                case 'developer-view':
                    loadDeveloperData();
                    break;
                case 'settings-view':
                    // Settings relies on initial loaded user state
                    break;
            }
        } catch (err) {
            App.showToast('Failed to load panel data.', 'danger');
        } finally {
            setTimeout(() => {
                if (loader) loader.style.setProperty('display', 'none', 'important'); // Hide loader
            }, 400);
        }
    }

    // --- VIEW ACTION 1: Overview Panel ---
    async function loadOverviewData() {
        try {
            const res = await App.fetchApi('/api/analytics');
            if (res.ok) {
                const data = await res.json();
                
                // Set stats
                document.getElementById('stat-total-links').textContent = data.totalLinks;
                document.getElementById('stat-total-clicks').textContent = data.totalClicks;
                document.getElementById('stat-active-links').textContent = data.activeLinks;
                document.getElementById('stat-expired-links').textContent = data.expiredLinks;

                // Populate Recent Activity Table
                const tbody = document.getElementById('recent-activity-tbody');
                tbody.innerHTML = '';
                
                if (data.recentClicks && data.recentClicks.length > 0) {
                    data.recentClicks.forEach(click => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td><span class="badge bg-secondary font-monospace">${click.shortCode}</span></td>
                            <td><i class="fas fa-map-marker-alt text-danger me-1"></i>${click.country}</td>
                            <td><i class="fas ${getDeviceIcon(click.device)} me-1"></i>${click.device}</td>
                            <td><i class="fas ${getBrowserIcon(click.browser)} me-1"></i>${click.browser}</td>
                            <td class="small text-muted">${click.clickTime}</td>
                        `;
                        tbody.appendChild(tr);
                    });
                } else {
                    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">No recent clicks recorded.</td></tr>`;
                }

                // Populate Health Monitors
                loadHealthMonitors();
            }
        } catch (e) {
            console.error('Error loading overview:', e);
        }
    }

    async function loadHealthMonitors() {
        const listContainer = document.getElementById('health-monitor-list');
        listContainer.innerHTML = '';

        try {
            // Fetch top 3 active links to monitor
            const res = await App.fetchApi('/api/urls?size=3&sortBy=createdAt');
            if (res.ok) {
                const data = await res.json();
                if (data.content && data.content.length > 0) {
                    data.content.forEach(url => {
                        const div = document.createElement('div');
                        div.className = 'glass-panel p-3 d-flex align-items-center justify-content-between border border-secondary-subtle';
                        div.innerHTML = `
                            <div>
                                <div class="font-weight-semibold font-monospace text-primary small">/r/${url.shortCode}</div>
                                <div class="text-truncate text-muted small" style="max-width: 200px;">${url.originalUrl}</div>
                            </div>
                            <span class="badge bg-secondary text-muted" id="health-badge-${url.id}"><i class="fas fa-spinner fa-spin me-1"></i>Checking...</span>
                        `;
                        listContainer.appendChild(div);
                        
                        // Run async check
                        checkIndividualHealth(url.id);
                    });
                } else {
                    listContainer.innerHTML = `<p class="text-muted small text-center my-4">Create short links to enable automated health monitoring.</p>`;
                }
            }
        } catch (e) {
            console.error(e);
        }
    }

    async function checkIndividualHealth(id) {
        const badge = document.getElementById(`health-badge-${id}`);
        try {
            const res = await App.fetchApi(`/api/urls/${id}/health`);
            if (res.ok) {
                const data = await res.json();
                if (data.status === 'HEALTHY') {
                    badge.className = 'badge bg-success-subtle text-success border border-success-subtle';
                    badge.innerHTML = '<i class="fas fa-check-circle me-1"></i>Healthy';
                } else {
                    badge.className = 'badge bg-danger-subtle text-danger border border-danger-subtle';
                    badge.innerHTML = `<i class="fas fa-exclamation-triangle me-1"></i>Broken`;
                }
            }
        } catch (e) {
            badge.className = 'badge bg-secondary text-muted';
            badge.innerHTML = 'Unknown';
        }
    }

    // --- VIEW ACTION 2: Link Manager Panel ---
    async function loadLinksData() {
        const tbody = document.getElementById('links-table-tbody');
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-muted">Retrieving campaigns database...</td></tr>';
        
        try {
            const queryUrl = `/api/urls?page=${currentPage}&size=${pageSize}&search=${encodeURIComponent(currentSearch)}&sortBy=${currentSortBy}&order=${currentOrder}`;
            const res = await App.fetchApi(queryUrl);
            if (res.ok) {
                const pageData = await res.json();
                tbody.innerHTML = '';
                
                if (pageData.content && pageData.content.length > 0) {
                    pageData.content.forEach(url => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>
                                <div class="fw-bold text-primary font-monospace">/r/${url.shortCode}</div>
                                <div class="small text-muted text-truncate" style="max-width: 320px;">${url.originalUrl}</div>
                                <div class="small text-muted mt-1"><i class="far fa-calendar-alt me-1"></i>${formatDate(url.createdAt)}</div>
                            </td>
                            <td><span class="fw-bold">${url.clicks}</span></td>
                            <td>
                                <div>${url.expiryDate ? `<span class="small text-secondary"><i class="far fa-clock me-1"></i>${formatDate(url.expiryDate)}</span>` : '<span class="text-muted small">No expiry date</span>'}</div>
                                <div class="mt-1">${url.passwordProtected ? '<span class="badge bg-danger-subtle text-danger"><i class="fas fa-lock me-1"></i>Locked</span>' : '<span class="text-muted small">Public link</span>'}</div>
                            </td>
                            <td>
                                <span class="${url.status === 'ACTIVE' ? 'badge-status-active' : url.status === 'EXPIRED' ? 'badge-status-expired' : 'badge-status-disabled'}">${url.status}</span>
                            </td>
                            <td>
                                <div class="d-flex gap-1">
                                    <button class="btn btn-sm btn-premium-outline copy-link-row-btn" data-url="${url.shortUrl}" title="Copy Link"><i class="fas fa-copy"></i></button>
                                    <button class="btn btn-sm btn-premium-outline qr-code-row-btn" data-qr="${url.qrCode}" title="QR Code"><i class="fas fa-qrcode"></i></button>
                                    <button class="btn btn-sm btn-premium-outline edit-row-btn" data-id="${url.id}" data-url="${url.originalUrl}" data-expiry="${url.expiryDate || ''}" data-clicks="${url.clickLimit || ''}" title="Edit"><i class="fas fa-edit"></i></button>
                                    <button class="btn btn-sm btn-premium-outline toggle-row-btn" data-id="${url.id}" title="Toggle status"><i class="fas fa-power-off"></i></button>
                                    <button class="btn btn-sm btn-outline-danger delete-row-btn" data-id="${url.id}" title="Delete"><i class="fas fa-trash"></i></button>
                                </div>
                            </td>
                        `;
                        tbody.appendChild(tr);
                    });
                    
                    // Bind row button events
                    bindLinkRowEvents();
                } else {
                    tbody.innerHTML = `<tr><td colspan="5" class="text-center py-5 text-muted">No links found matching your query criteria.</td></tr>`;
                }

                // Update pagination controls
                updatePagination(pageData);
            }
        } catch (e) {
            console.error('Error fetching URLs:', e);
        }
    }

    function bindLinkRowEvents() {
        // Copy link action
        document.querySelectorAll('.copy-link-row-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                App.copyToClipboard(btn.getAttribute('data-url'), btn);
            });
        });

        // Open QR Modal
        document.querySelectorAll('.qr-code-row-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const qrBase64 = btn.getAttribute('data-qr');
                const modalImg = document.getElementById('qr-modal-image');
                const downloadBtn = document.getElementById('qr-modal-download-btn');
                
                modalImg.src = `data:image/png;base64,${qrBase64}`;
                downloadBtn.href = `data:image/png;base64,${qrBase64}`;
                
                const qrModal = new bootstrap.Modal(document.getElementById('qrCodeModal'));
                qrModal.show();
            });
        });

        // Toggle Active / Disabled state
        document.querySelectorAll('.toggle-row-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-id');
                const res = await App.fetchApi(`/api/urls/${id}/toggle`, { method: 'POST' });
                if (res.ok) {
                    App.showToast('Link status updated!');
                    loadLinksData();
                }
            });
        });

        // Delete Row action
        document.querySelectorAll('.delete-row-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                if (confirm('Are you sure you want to permanently delete this shortened link campaign?')) {
                    const id = btn.getAttribute('data-id');
                    const res = await App.fetchApi(`/api/urls/${id}`, { method: 'DELETE' });
                    if (res.ok) {
                        App.showToast('Link deleted successfully!');
                        loadLinksData();
                    }
                }
            });
        });

        // Open Edit Modal and prefill settings
        document.querySelectorAll('.edit-row-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const id = btn.getAttribute('data-id');
                const originalUrl = btn.getAttribute('data-url');
                const expiry = btn.getAttribute('data-expiry');
                const clicks = btn.getAttribute('data-clicks');
                
                document.getElementById('edit-url-id').value = id;
                document.getElementById('edit-originalUrl').value = originalUrl;
                document.getElementById('edit-expiryDate').value = expiry ? expiry.substring(0, 16) : '';
                document.getElementById('edit-clickLimit').value = clicks;
                document.getElementById('edit-password').value = ''; // Don't prefill password hash
                
                const editModal = new bootstrap.Modal(document.getElementById('editUrlModal'));
                editModal.show();
            });
        });
    }

    function updatePagination(pageData) {
        const prevBtn = document.getElementById('prev-page-btn');
        const nextBtn = document.getElementById('next-page-btn');
        const pageIndicator = document.getElementById('page-indicator');

        prevBtn.disabled = pageData.first;
        nextBtn.disabled = pageData.last;
        pageIndicator.textContent = `Page ${pageData.number + 1} of ${Math.max(1, pageData.totalPages)}`;
    }

    // Apply Filter actions
    document.getElementById('search-filter-btn').addEventListener('click', () => {
        currentSearch = document.getElementById('search-links-input').value;
        currentSortBy = document.getElementById('sort-links-select').value;
        currentPage = 0;
        loadLinksData();
    });

    // Page switcher triggers
    document.getElementById('prev-page-btn').addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            loadLinksData();
        }
    });

    document.getElementById('next-page-btn').addEventListener('click', () => {
        currentPage++;
        loadLinksData();
    });

    // --- VIEW ACTION 3: Analytics Dashboard Charting ---
    async function loadAnalyticsData() {
        try {
            const res = await App.fetchApi('/api/analytics');
            if (res.ok) {
                const data = await res.json();
                
                // Draw line and pie charts using Chart.js helper
                renderTimelineChart(data.clicksByDate);
                renderDeviceChart(data.clicksByDevice);
                renderBrowserChart(data.clicksByBrowser);
                
                // Populate referrers table
                const refTbody = document.getElementById('referrers-analytics-tbody');
                refTbody.innerHTML = '';
                if (data.clicksByReferrer && Object.keys(data.clicksByReferrer).length > 0) {
                    // Sort referrers count descending
                    const sortedRefs = Object.entries(data.clicksByReferrer)
                        .sort((a,b) => b[1] - a[1]);
                    
                    sortedRefs.forEach(([ref, clicks]) => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td><i class="fas fa-globe text-primary me-2"></i>${ref}</td>
                            <td class="text-end fw-bold">${clicks}</td>
                        `;
                        refTbody.appendChild(tr);
                    });
                } else {
                    refTbody.innerHTML = '<tr><td colspan="2" class="text-center py-4">No referrer records.</td></tr>';
                }

                // Populate Geographic Table
                const geoTbody = document.getElementById('geo-analytics-tbody');
                geoTbody.innerHTML = '';
                if (data.clicksByCountry && Object.keys(data.clicksByCountry).length > 0) {
                    const sortedGeo = Object.entries(data.clicksByCountry)
                        .sort((a,b) => b[1] - a[1]);
                    
                    sortedGeo.forEach(([country, clicks]) => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td><i class="fas fa-map-marker-alt text-danger me-2"></i>${country}</td>
                            <td class="text-end fw-bold">${clicks}</td>
                        `;
                        geoTbody.appendChild(tr);
                    });
                } else {
                    geoTbody.innerHTML = '<tr><td colspan="2" class="text-center py-4">No geographical records.</td></tr>';
                }

                // Populate Top Performing links table
                const topLinksTbody = document.getElementById('top-links-tbody');
                topLinksTbody.innerHTML = '';
                if (data.topLinks && data.topLinks.length > 0) {
                    data.topLinks.forEach(link => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td><span class="badge bg-secondary font-monospace">/r/${link.shortCode}</span></td>
                            <td class="text-muted text-truncate" style="max-width: 400px;">${link.originalUrl}</td>
                            <td class="text-end fw-bold">${link.clicks} clicks</td>
                        `;
                        topLinksTbody.appendChild(tr);
                    });
                } else {
                    topLinksTbody.innerHTML = '<tr><td colspan="3" class="text-center py-3 text-muted">No links created yet.</td></tr>';
                }
            }
        } catch (err) {
            console.error('Error drawing charts:', err);
        }
    }

    // Chart.js render configurations
    function renderTimelineChart(dateData) {
        if (charts.timeline) charts.timeline.destroy();
        
        const labels = Object.keys(dateData);
        const clicks = Object.values(dateData);
        
        const ctx = document.getElementById('dailyClicksChart').getContext('2d');
        const theme = document.documentElement.getAttribute('data-theme');
        const textColor = theme === 'dark' ? '#9ca3af' : '#475569';
        const gridColor = theme === 'dark' ? 'rgba(55,65,81,0.2)' : 'rgba(226,232,240,0.5)';

        charts.timeline = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Clicks',
                    data: clicks,
                    borderColor: '#6366f1',
                    borderWidth: 3,
                    backgroundColor: 'rgba(99,102,241,0.1)',
                    fill: true,
                    tension: 0.35,
                    pointBackgroundColor: '#4f46e5',
                    pointBorderColor: '#ffffff',
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    x: {
                        grid: { color: gridColor },
                        ticks: { color: textColor }
                    },
                    y: {
                        beginAtZero: true,
                        grid: { color: gridColor },
                        ticks: { color: textColor }
                    }
                }
            }
        });
    }

    function renderDeviceChart(deviceData) {
        if (charts.devices) charts.devices.destroy();
        const ctx = document.getElementById('deviceChart').getContext('2d');
        const labels = Object.keys(deviceData);
        const data = Object.values(deviceData);

        charts.devices = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: ['#6366f1', '#10b981', '#f59e0b', '#06b6d4', '#6b7280'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            color: document.documentElement.getAttribute('data-theme') === 'dark' ? '#9ca3af' : '#475569'
                        }
                    }
                }
            }
        });
    }

    function renderBrowserChart(browserData) {
        if (charts.browsers) charts.browsers.destroy();
        const ctx = document.getElementById('browserChart').getContext('2d');
        const labels = Object.keys(browserData);
        const data = Object.values(browserData);

        charts.browsers = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: ['#3b82f6', '#ef4444', '#f59e0b', '#10b981', '#8b5cf6', '#6b7280'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            color: document.documentElement.getAttribute('data-theme') === 'dark' ? '#9ca3af' : '#475569'
                        }
                    }
                }
            }
        });
    }

    // Trigger explicit CSV Export redirect
    document.getElementById('export-analytics-btn').addEventListener('click', () => {
        window.location.href = '/api/analytics/export';
    });
    
    // Refresh action
    document.getElementById('refresh-analytics-btn').addEventListener('click', () => {
        loadAnalyticsData();
        App.showToast('Analytics dataset refreshed!');
    });

    // --- VIEW ACTION 4: Developer Panel ---
    function loadDeveloperData() {
        const keyText = document.getElementById('developer-api-key-text');
        keyText.value = currentUser.apiKey;
    }

    // Toggle Masking key
    const toggleRevealBtn = document.getElementById('toggle-reveal-key-btn');
    if (toggleRevealBtn) {
        toggleRevealBtn.addEventListener('click', () => {
            const keyText = document.getElementById('developer-api-key-text');
            const icon = toggleRevealBtn.querySelector('i');
            if (keyText.type === 'password') {
                keyText.type = 'text';
                icon.className = 'fas fa-eye-slash';
            } else {
                keyText.type = 'password';
                icon.className = 'fas fa-eye';
            }
        });
    }

    // Copy API Key
    document.getElementById('copy-api-key-btn').addEventListener('click', (e) => {
        const keyText = document.getElementById('developer-api-key-text');
        App.copyToClipboard(keyText.value, e.currentTarget);
    });

    // Regenerate API Key
    document.getElementById('regenerate-api-key-btn').addEventListener('click', async () => {
        if (confirm('Regenerating will instantly invalidate your current API key. Developers utilizing this key in external services will lose connectivity immediately. Proceed?')) {
            try {
                const res = await App.fetchApi('/api/auth/apikey/regenerate', { method: 'POST' });
                if (res.ok) {
                    const data = await res.json();
                    currentUser.apiKey = data.apiKey;
                    document.getElementById('developer-api-key-text').value = data.apiKey;
                    App.showToast('Developer API Token updated!');
                }
            } catch (err) {
                App.showToast('Failed to regenerate key.', 'danger');
            }
        }
    });

    // --- CRUD OPERATION ACTIONS ---
    
    // Create Short URL submit
    const createForm = document.getElementById('create-url-form');
    if (createForm) {
        createForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const originalUrl = document.getElementById('modal-originalUrl').value;
            const customAlias = document.getElementById('modal-customAlias').value;
            const expiryDate = document.getElementById('modal-expiryDate').value;
            const clickLimit = document.getElementById('modal-clickLimit').value;
            const password = document.getElementById('modal-password').value;

            const payload = {
                originalUrl,
                customAlias: customAlias ? customAlias : null,
                expiryDate: expiryDate ? new Date(expiryDate).toISOString().substring(0,19) : null,
                clickLimit: clickLimit ? parseInt(clickLimit) : null,
                password: password ? password : null
            };

            try {
                const res = await App.fetchApi('/api/urls', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });

                if (res.ok) {
                    App.showToast('Short Link generated successfully!');
                    createForm.reset();
                    bootstrap.Modal.getInstance(document.getElementById('createUrlModal')).hide();
                    
                    // Reload appropriate views
                    if (currentView === 'overview-view') loadOverviewData();
                    if (currentView === 'links-view') loadLinksData();
                } else {
                    const err = await res.json();
                    App.showToast(err.error || 'Failed to generate link.', 'danger');
                }
            } catch (err) {
                App.showToast('Connection error.', 'danger');
            }
        });
    }

    // Edit settings update submit
    const editForm = document.getElementById('edit-url-form');
    if (editForm) {
        editForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('edit-url-id').value;
            const expiryDate = document.getElementById('edit-expiryDate').value;
            const clickLimit = document.getElementById('edit-clickLimit').value;
            const password = document.getElementById('edit-password').value;

            const payload = {
                expiryDate: expiryDate ? new Date(expiryDate).toISOString().substring(0,19) : null,
                clickLimit: clickLimit ? parseInt(clickLimit) : null,
                password: password // If empty, backend clears password lock
            };

            try {
                const res = await App.fetchApi(`/api/urls/${id}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });

                if (res.ok) {
                    App.showToast('URL configuration updated!');
                    editForm.reset();
                    bootstrap.Modal.getInstance(document.getElementById('editUrlModal')).hide();
                    loadLinksData();
                } else {
                    const err = await res.json();
                    App.showToast(err.error || 'Failed to update URL.', 'danger');
                }
            } catch (err) {
                App.showToast('Connection error.', 'danger');
            }
        });
    }

    // Bulk Shortener form submit
    const bulkForm = document.getElementById('bulk-shorten-form');
    if (bulkForm) {
        bulkForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const text = document.getElementById('bulk-urls-textarea').value;
            const originalUrls = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);

            if (originalUrls.length === 0) {
                App.showToast('Please insert at least one link.', 'danger');
                return;
            }

            try {
                const res = await App.fetchApi('/api/urls/bulk', {
                    method: 'POST',
                    body: JSON.stringify({ originalUrls })
                });

                if (res.ok) {
                    const data = await res.json();
                    const count = data.shortened.length;
                    App.showToast(`Successfully processed ${count} short links!`);
                    
                    bulkForm.reset();
                    bootstrap.Modal.getInstance(document.getElementById('bulkUrlModal')).hide();
                    
                    if (currentView === 'overview-view') loadOverviewData();
                    if (currentView === 'links-view') loadLinksData();
                } else {
                    App.showToast('Bulk shortening failed.', 'danger');
                }
            } catch (err) {
                App.showToast('Network error.', 'danger');
            }
        });
    }

    // Profile Settings Form Submit
    const profileForm = document.getElementById('update-profile-form');
    if (profileForm) {
        profileForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('settings-email').value;
            const password = document.getElementById('settings-new-password').value;

            try {
                const res = await App.fetchApi('/api/auth/profile', {
                    method: 'PUT',
                    body: JSON.stringify({ email, password })
                });

                if (res.ok) {
                    const data = await res.json();
                    App.showToast('Security profile updated successfully!');
                    currentUser.email = data.email;
                    document.getElementById('settings-new-password').value = '';
                } else {
                    const err = await res.json();
                    App.showToast(err.error || 'Failed to update settings profile.', 'danger');
                }
            } catch (err) {
                App.showToast('Connection error.', 'danger');
            }
        });
    }

    // --- HELPER FORMATTING FUNCTIONS ---
    function formatDate(isoString) {
        if (!isoString) return '';
        const d = new Date(isoString);
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    function getDeviceIcon(device) {
        switch(device) {
            case 'Mobile': return 'fa-mobile-alt';
            case 'Tablet': return 'fa-tablet-alt';
            default: return 'fa-desktop';
        }
    }

    function getBrowserIcon(browser) {
        switch(browser) {
            case 'Chrome': return 'fa-chrome text-primary';
            case 'Firefox': return 'fa-firefox-browser text-warning';
            case 'Safari': return 'fa-safari text-info';
            case 'Edge': return 'fa-edge text-primary';
            case 'Opera': return 'fa-opera text-danger';
            default: return 'fa-globe text-muted';
        }
    }

    // Trigger Initial load
    loadOverviewData();
});
