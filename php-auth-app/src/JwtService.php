<?php

namespace App;

use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Exception;

class JwtService
{
    private string $privateKey;
    private string $publicKey;
    private string $algorithm;
    private string $issuer;
    private int $expirySeconds;

    public function __construct()
    {
        $config = require __DIR__ . '/../config/jwt.php';

        $this->privateKey   = file_get_contents($config['private_key_path']);
        $this->publicKey    = file_get_contents($config['public_key_path']);
        $this->algorithm    = $config['algorithm'];
        $this->issuer       = $config['issuer'];
        $this->expirySeconds = $config['expiry_seconds'];
    }

    /**
     * Generate a JWT for the given user.
     */
    public function encode(array $user): string
    {
        $now = time();

        $payload = [
            'iss'   => $this->issuer,
            'iat'   => $now,
            'exp'   => $now + $this->expirySeconds,
            'sub'   => (string) $user['id'],
            'email' => $user['email'],
            'name'  => $user['name'],
        ];

        return JWT::encode($payload, $this->privateKey, $this->algorithm);
    }

    /**
     * Decode and validate a JWT. Returns the claims as an associative array.
     *
     * @throws Exception if the token is invalid or expired.
     */
    public function decode(string $token): array
    {
        try {
            $decoded = JWT::decode($token, new Key($this->publicKey, $this->algorithm));
            return (array) $decoded;
        } catch (Exception $e) {
            throw new Exception('Invalid token: ' . $e->getMessage());
        }
    }
}
