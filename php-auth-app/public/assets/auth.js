// ============================================
// JWT Auth - Frontend Token Management
// ============================================

const AUTH_API = '';  // Same origin
const JAVA_APP_URL = 'http://localhost:8081/java-api-app';
const TOKEN_KEY = 'jwt_token';
const USER_KEY = 'jwt_user';

// --- Token Storage ---
function getToken() { return localStorage.getItem(TOKEN_KEY); }
function setToken(token) { localStorage.setItem(TOKEN_KEY, token); }
function getUser() { try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; } }
function setUser(user) { localStorage.setItem(USER_KEY, JSON.stringify(user)); }
function clearAuth() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); }

// --- Check if token exists in URL hash (coming back from another app) ---
function checkHashToken() {
    const hash = window.location.hash;
    if (hash.startsWith('#token=')) {
        const token = hash.substring(7);
        setToken(token);
        history.replaceState(null, '', window.location.pathname);
    }
}

// --- API Helper ---
async function apiCall(method, url, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(AUTH_API + url, opts);
    const data = await res.json();
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

function requireAuth() {
    if (!getToken()) {
        window.location.href = '/';
        return false;
    }
    return true;
}

function requireGuest() {
    if (getToken()) {
        window.location.href = '/dashboard';
        return false;
    }
    return true;
}

// --- Navigation ---
function navigateToProducts() {
    const token = getToken();
    window.location.href = JAVA_APP_URL + '/#token=' + token;
}

function logout() {
    clearAuth();
    // Also clear Java app's token by redirecting through it
    window.location.href = JAVA_APP_URL + '/#logout';
}

// --- Update navbar user info ---
function updateNavbar() {
    const user = getUser();
    const el = document.getElementById('nav-user');
    if (el && user) el.textContent = user.name || user.email;
}
