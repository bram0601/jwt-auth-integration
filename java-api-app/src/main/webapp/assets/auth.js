// Java Auth App - Frontend Token Management
const PHP_APP_URL = 'http://localhost:8080';
const TOKEN_KEY = 'jwt_token';
const USER_KEY = 'jwt_user';

function getToken() { return localStorage.getItem(TOKEN_KEY); }
function setToken(token) { localStorage.setItem(TOKEN_KEY, token); }
function getUser() { try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; } }
function setUser(user) { localStorage.setItem(USER_KEY, JSON.stringify(user)); }
function clearAuth() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); }

function checkHashToken() {
    const hash = window.location.hash;
    if (hash.startsWith('#token=')) {
        setToken(hash.substring(7));
        history.replaceState(null, '', window.location.pathname);
    }
}

async function apiCall(method, url, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(url, opts);
    const data = await res.json();
    if (!res.ok) throw { status: res.status, ...data };
    return data;
}

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

function navigateToProducts() {
    window.location.href = PHP_APP_URL + '/#token=' + getToken();
}

function doLogout() {
    clearAuth();
    window.location.href = PHP_APP_URL + '/#logout';
}
