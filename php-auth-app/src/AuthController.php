<?php

namespace App;

use PDO;
use Exception;

class AuthController
{
    private PDO $db;
    private JwtService $jwt;

    public function __construct()
    {
        $this->db  = Database::getAuthConnection();
        $this->jwt = new JwtService();
    }

    /**
     * POST /register
     * Body JSON: { "email": "...", "password": "...", "name": "..." }
     */
    public function register(): void
    {
        $input = json_decode(file_get_contents('php://input'), true);

        if (empty($input['email']) || empty($input['password']) || empty($input['name'])) {
            $this->jsonResponse(400, ['error' => 'email, password, and name are required']);
            return;
        }

        $email    = trim($input['email']);
        $password = $input['password'];
        $name     = trim($input['name']);

        // Check if email already exists
        $stmt = $this->db->prepare('SELECT id FROM users WHERE email = :email');
        $stmt->execute(['email' => $email]);

        if ($stmt->fetch()) {
            $this->jsonResponse(409, ['error' => 'Email already registered']);
            return;
        }

        $passwordHash = password_hash($password, PASSWORD_BCRYPT);

        $stmt = $this->db->prepare(
            'INSERT INTO users (email, password_hash, name) VALUES (:email, :password_hash, :name) RETURNING id'
        );
        $stmt->execute([
            'email'         => $email,
            'password_hash' => $passwordHash,
            'name'          => $name,
        ]);

        $userId = $stmt->fetchColumn();

        $this->jsonResponse(201, [
            'message' => 'User registered successfully',
            'user'    => [
                'id'    => (int) $userId,
                'email' => $email,
                'name'  => $name,
            ],
        ]);
    }

    /**
     * POST /login
     * Body JSON: { "email": "...", "password": "..." }
     */
    public function login(): void
    {
        $input = json_decode(file_get_contents('php://input'), true);

        if (empty($input['email']) || empty($input['password'])) {
            $this->jsonResponse(400, ['error' => 'email and password are required']);
            return;
        }

        $stmt = $this->db->prepare('SELECT id, email, password_hash, name FROM users WHERE email = :email');
        $stmt->execute(['email' => trim($input['email'])]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($input['password'], $user['password_hash'])) {
            $this->jsonResponse(401, ['error' => 'Invalid email or password']);
            return;
        }

        $token = $this->jwt->encode($user);

        $this->jsonResponse(200, [
            'message' => 'Login successful',
            'token'   => $token,
            'user'    => [
                'id'    => (int) $user['id'],
                'email' => $user['email'],
                'name'  => $user['name'],
            ],
        ]);
    }

    /**
     * GET /me
     * Requires Authorization: Bearer <token>
     */
    public function me(): void
    {
        $token = $this->extractBearerToken();

        if (!$token) {
            $this->jsonResponse(401, ['error' => 'Authorization header with Bearer token required']);
            return;
        }

        try {
            $claims = $this->jwt->decode($token);
        } catch (Exception $e) {
            $this->jsonResponse(401, ['error' => $e->getMessage()]);
            return;
        }

        // Fetch fresh user data from DB
        $stmt = $this->db->prepare('SELECT id, email, name, created_at FROM users WHERE id = :id');
        $stmt->execute(['id' => $claims['sub']]);
        $user = $stmt->fetch();

        if (!$user) {
            $this->jsonResponse(404, ['error' => 'User not found']);
            return;
        }

        $this->jsonResponse(200, [
            'user' => [
                'id'         => (int) $user['id'],
                'email'      => $user['email'],
                'name'       => $user['name'],
                'created_at' => $user['created_at'],
            ],
        ]);
    }

    /**
     * Extract Bearer token from Authorization header.
     */
    private function extractBearerToken(): ?string
    {
        $header = $_SERVER['HTTP_AUTHORIZATION']
            ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
            ?? '';

        if (preg_match('/^Bearer\s+(.+)$/i', $header, $matches)) {
            return $matches[1];
        }

        return null;
    }

    /**
     * Send a JSON response.
     */
    private function jsonResponse(int $statusCode, array $data): void
    {
        http_response_code($statusCode);
        echo json_encode($data, JSON_UNESCAPED_UNICODE);
    }
}
