// ============================================
// Products App - Frontend JS
// ============================================

const API_BASE = 'api';
const AUTH_APP_URL = 'http://localhost:8080';
const TOKEN_KEY = 'jwt_token';

// --- Token Management ---
function getToken() { return localStorage.getItem(TOKEN_KEY); }
function setToken(token) { localStorage.setItem(TOKEN_KEY, token); }
function clearToken() { localStorage.removeItem(TOKEN_KEY); }

// --- Handle incoming token from PHP app via URL hash ---
function initAuth() {
    const hash = window.location.hash;

    // Handle logout request from PHP app
    if (hash === '#logout') {
        clearToken();
        history.replaceState(null, '', '.');
        window.location.href = AUTH_APP_URL + '/#logged-out';
        return false;
    }

    // Handle token passed from PHP app
    if (hash.startsWith('#token=')) {
        const token = hash.substring(7);
        setToken(token);
        history.replaceState(null, '', '.');
    }

    // No token? Redirect to login
    if (!getToken()) {
        window.location.href = AUTH_APP_URL;
        return false;
    }

    return true;
}

// --- API Helper ---
async function apiCall(method, url, body) {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
    };

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(API_BASE + '/' + url, opts);
    const data = await res.json();

    if (res.status === 401) {
        clearToken();
        window.location.href = AUTH_APP_URL;
        return;
    }

    if (!res.ok) throw { status: res.status, ...data };
    return data;
}

// --- UI Helpers ---
function showAlert(id, message, type) {
    const el = document.getElementById(id);
    if (!el) return;
    el.className = 'alert alert-' + type + ' show';
    el.textContent = message;
}

function hideAlert(id) {
    const el = document.getElementById(id);
    if (el) el.className = 'alert';
}

// --- Load Products ---
async function loadProducts() {
    try {
        const data = await apiCall('GET', 'products');
        renderProducts(data.products || []);
    } catch (err) {
        showAlert('alert', err.error || 'Failed to load products', 'error');
    }
}

function renderProducts(products) {
    const el = document.getElementById('productList');
    if (products.length === 0) {
        el.innerHTML = '<div class="empty-state">No products yet. Click "+ New Product" to create one.</div>';
        return;
    }

    el.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Price</th>
                    <th>Created By</th>
                    <th>Date</th>
                </tr>
            </thead>
            <tbody>
                ${products.map(p => `
                    <tr>
                        <td>${p.id}</td>
                        <td><strong>${escapeHtml(p.name)}</strong></td>
                        <td>${escapeHtml(p.description || '')}</td>
                        <td>$${Number(p.price).toFixed(2)}</td>
                        <td>User #${p.created_by}</td>
                        <td>${new Date(p.created_at).toLocaleDateString()}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// --- Create Product Modal ---
function openCreateModal() {
    document.getElementById('createModal').classList.add('show');
    document.getElementById('prodName').focus();
    hideAlert('modalAlert');
}

function closeCreateModal() {
    document.getElementById('createModal').classList.remove('show');
    document.getElementById('createForm').reset();
}

document.getElementById('createForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert('modalAlert');
    const btn = document.getElementById('createBtn');
    btn.disabled = true;
    btn.textContent = 'Creating...';

    try {
        await apiCall('POST', 'products-create', {
            name: document.getElementById('prodName').value,
            description: document.getElementById('prodDesc').value,
            price: parseFloat(document.getElementById('prodPrice').value)
        });
        closeCreateModal();
        showAlert('alert', 'Product created successfully!', 'success');
        setTimeout(() => hideAlert('alert'), 3000);
        loadProducts();
    } catch (err) {
        showAlert('modalAlert', err.error || 'Failed to create product', 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Create Product';
    }
});

// --- Navigation ---
function doLogout() {
    clearToken();
    window.location.href = AUTH_APP_URL + '/logout';
}

// Update dashboard link to pass token back
document.getElementById('nav-dashboard').addEventListener('click', function(e) {
    e.preventDefault();
    window.location.href = AUTH_APP_URL + '/dashboard#token=' + getToken();
});

// --- Initialize ---
if (initAuth()) {
    loadProducts();
}
