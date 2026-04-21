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

// HTML page routes
if ($method === 'GET' && !str_starts_with($uri, '/api/')) {
    switch ($uri) {
        case '/':
            readfile(__DIR__ . '/views/products.html');
            exit;
        default:
            break;
    }
}

// JSON API routes
header('Content-Type: application/json');

$productController = new \App\ProductController();
$authController    = new \App\AuthController();

switch (true) {
    // Product API (JWT protected — validated inside ProductController)
    case $method === 'GET' && $uri === '/api/products' && !isset($_GET['id']):
        $productController->list();
        break;

    case $method === 'GET' && $uri === '/api/products' && isset($_GET['id']):
        $productController->get();
        break;

    case $method === 'POST' && $uri === '/api/products':
        $productController->create();
        break;

    // Legacy auth API (still available)
    case $method === 'POST' && $uri === '/register':
        $authController->register();
        break;

    case $method === 'POST' && $uri === '/login':
        $authController->login();
        break;

    case $method === 'GET' && $uri === '/me':
        $authController->me();
        break;

    default:
        http_response_code(404);
        echo json_encode(['error' => 'Not found']);
        break;
}
