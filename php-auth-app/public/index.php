<?php

require_once __DIR__ . '/../vendor/autoload.php';

// Parse request
$method = $_SERVER['REQUEST_METHOD'];
$uri    = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$uri    = rtrim($uri, '/') ?: '/';

// Serve static assets directly
if (preg_match('/^\/(assets)\//', $uri)) {
    return false; // Let PHP built-in server handle static files
}

// HTML page routes (GET requests serve HTML views)
if ($method === 'GET') {
    switch ($uri) {
        case '/':
            readfile(__DIR__ . '/views/login.html');
            exit;
        case '/register':
            readfile(__DIR__ . '/views/register.html');
            exit;
        case '/dashboard':
            readfile(__DIR__ . '/views/dashboard.html');
            exit;
        case '/logout':
            // Serve a simple page that clears localStorage and redirects
            echo '<!DOCTYPE html><html><body><script>
                localStorage.removeItem("jwt_token");
                localStorage.removeItem("jwt_user");
                window.location.href = "/";
            </script></body></html>';
            exit;
        case '/me':
            // GET /me is an API endpoint — fall through to JSON handler
            break;
        default:
            break;
    }
}

// JSON API routes
header('Content-Type: application/json');

$controller = new \App\AuthController();

switch (true) {
    case $method === 'POST' && $uri === '/register':
        $controller->register();
        break;

    case $method === 'POST' && $uri === '/login':
        $controller->login();
        break;

    case $method === 'GET' && $uri === '/me':
        $controller->me();
        break;

    default:
        http_response_code(404);
        echo json_encode(['error' => 'Not found']);
        break;
}
