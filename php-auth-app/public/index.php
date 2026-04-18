<?php

require_once __DIR__ . '/../vendor/autoload.php';

header('Content-Type: application/json');

// Parse request
$method = $_SERVER['REQUEST_METHOD'];
$uri    = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

// Remove trailing slash
$uri = rtrim($uri, '/');

$controller = new \App\AuthController();

// Simple router
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
