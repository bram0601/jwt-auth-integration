// PHP Products App - Frontend JS
const JAVA_APP_URL = 'http://localhost:8081/java-api-app';
const TOKEN_KEY = 'jwt_token';

function getToken() { return localStorage.getItem(TOKEN_KEY); }
function setToken(token) { localStorage.setItem(TOKEN_KEY, token); }
function clearToken() { localStorage.removeItem(TOKEN_KEY); }

// Handle incoming token/logout from Java app via URL hash
function initAuth() {
    const hash = window.location.hash;

    if (hash === '#logout') {
        clearToken();
        history.replaceState(null, '', '/');
        window.location.href = JAVA_APP_URL + '/login.html#logged-out';
        return false;
    }

    if (hash.startsWith('#token=')) {
        setToken(hash.substring(7));
        history.replaceState(null, '', '/');
    }

    if (!getToken()) {
        window.location.href = JAVA_APP_URL + '/login.html';
        return false;
    }
    return true;
}

async function apiCall(method, url, body) {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
    };
    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(url, opts);
    const data = await res.json();
    if (res.status === 401) {
        clearToken();
        window.location.href = JAVA_APP_URL + '/login.html';
        return;
    }
    if (!res.ok) throw { status: res.status, ...data };
    return data;
}

function showAlert(id, msg, type) {
    const el = document.getElementById(id);
    if (!el) return;
    el.className = 'alert alert-' + type + ' show';
    el.textContent = msg;
}
function hideAlert(id) {
    const el = document.getElementById(id);
    if (el) el.className = 'alert';
}

async function loadProducts() {
    try {
        const data = await apiCall('GET', '/api/products');
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
    el.innerHTML = '<table><thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Price</th><th>Created By</th><th>Date</th></tr></thead><tbody>' +
        products.map(p =>
            '<tr><td>' + p.id + '</td><td><strong>' + esc(p.name) + '</strong></td><td>' + esc(p.description||'') + '</td><td>$' + Number(p.price).toFixed(2) + '</td><td>User #' + p.created_by + '</td><td>' + new Date(p.created_at).toLocaleDateString() + '</td></tr>'
        ).join('') + '</tbody></table>';
}

function esc(t) { const d = document.createElement('div'); d.textContent = t; return d.innerHTML; }

function openCreateModal() { document.getElementById('createModal').classList.add('show'); hideAlert('modalAlert'); }
function closeCreateModal() { document.getElementById('createModal').classList.remove('show'); document.getElementById('createForm').reset(); }

document.getElementById('createForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert('modalAlert');
    const btn = document.getElementById('createBtn');
    btn.disabled = true; btn.textContent = 'Creating...';
    try {
        await apiCall('POST', '/api/products', {
            name: document.getElementById('prodName').value,
            description: document.getElementById('prodDesc').value,
            price: parseFloat(document.getElementById('prodPrice').value)
        });
        closeCreateModal();
        showAlert('alert', 'Product created!', 'success');
        setTimeout(() => hideAlert('alert'), 3000);
        loadProducts();
    } catch (err) {
        showAlert('modalAlert', err.error || 'Failed to create product', 'error');
    } finally { btn.disabled = false; btn.textContent = 'Create Product'; }
});

function navigateToDashboard() {
    window.location.href = JAVA_APP_URL + '/dashboard.html#token=' + getToken();
}

function doLogout() {
    clearToken();
    window.location.href = JAVA_APP_URL + '/login.html';
}

if (initAuth()) loadProducts();
