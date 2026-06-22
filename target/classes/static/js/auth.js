// Authentication Handlers for ShortLink Pro
document.addEventListener('DOMContentLoaded', () => {
    
    // 1. Handle Login Form Submit
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        // Check if redirected due to expired session
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('session_expired') === 'true') {
            App.showToast('Your session has expired. Please sign in again.', 'info');
        }

        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;

            try {
                const response = await App.fetchApi('/api/auth/login', {
                    method: 'POST',
                    body: JSON.stringify({ username, password })
                });

                if (response.ok) {
                    const data = await response.json();
                    App.showToast('Login successful! Welcome back.');
                    setTimeout(() => {
                        window.location.href = '/dashboard.html';
                    }, 1000);
                } else {
                    const errData = await response.json();
                    App.showToast(errData.error || 'Invalid username or password.', 'danger');
                }
            } catch (err) {
                App.showToast('Network error. Please try again.', 'danger');
                console.error(err);
            }
        });
    }

    // 2. Handle Registration Form Submit
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;

            try {
                const response = await App.fetchApi('/api/auth/register', {
                    method: 'POST',
                    body: JSON.stringify({ username, email, password })
                });

                if (response.ok) {
                    App.showToast('Registration successful! Redirecting to login...');
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 1500);
                } else {
                    const errData = await response.json();
                    App.showToast(errData.error || 'Failed to register account.', 'danger');
                }
            } catch (err) {
                App.showToast('Network error. Please try again.', 'danger');
                console.error(err);
            }
        });
    }

    // 3. Handle Forgot Password Form Submit (Simulated)
    const forgotForm = document.getElementById('forgot-form');
    if (forgotForm) {
        forgotForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const email = document.getElementById('email').value;
            App.showToast(`Instructions sent to ${email}! Check inbox.`);
            document.getElementById('email').value = '';
        });
    }
});
