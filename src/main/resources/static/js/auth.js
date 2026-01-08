const API_URL = '/api/v1';

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const alertBox = document.getElementById('alertMessage');

    function showAlert(message, type) {
        alertBox.textContent = message;
        alertBox.className = `alert alert-${type}`;
        setTimeout(() => alertBox.classList.add('hidden'), 5000);
    }

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('email').value;
            // NOTE: Simple mock login for demo - In real app use /auth endpoint
            // Here we check if member exists by fetching logic or simulating
            try {
                // Determine user ID (Mock simulation)
                // For PROD: POST /login -> JWT
                // For DEMO: We will first try to find member by email or just store email in localstorage
                localStorage.setItem('userEmail', email);
                localStorage.setItem('userId', '1'); // hardcoded for demo simplicity or fetch from member list
                window.location.href = 'dashboard.html';
            } catch (err) {
                showAlert('Login failed. Please check credentials.', 'error');
            }
        });
    }

    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                firstName: document.getElementById('firstName').value,
                lastName: document.getElementById('lastName').value,
                email: document.getElementById('email').value,
                phone: document.getElementById('phone').value
            };

            try {
                const response = await fetch(`${API_URL}/members`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (response.ok) {
                    showAlert('Account created! Redirecting to login...', 'success');
                    setTimeout(() => window.location.href = 'index.html', 2000);
                } else {
                    const err = await response.json();
                    showAlert(err.message || 'Registration failed', 'error');
                }
            } catch (error) {
                showAlert('System error occurred', 'error');
            }
        });
    }
});
