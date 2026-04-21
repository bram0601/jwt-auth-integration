<?php

namespace App;

use PDO;
use Exception;

class ProductController
{
    private PDO $db;
    private JwtService $jwt;

    public function __construct()
    {
        $this->db  = Database::getConnection();
        $this->jwt = new JwtService();
    }

    /**
     * Validate JWT from Authorization header.
     * Returns userId on success, sends 401 and returns null on failure.
     */
    private function authenticate(): ?string
    {
        $header = $_SERVER['HTTP_AUTHORIZATION']
            ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
            ?? '';

        if (!preg_match('/^Bearer\s+(.+)$/i', $header, $matches)) {
            $this->jsonResponse(401, ['error' => 'Authorization header with Bearer token required']);
            return null;
        }

        try {
            $claims = $this->jwt->decode($matches[1]);
            return (string) $claims['sub'];
        } catch (Exception $e) {
            $this->jsonResponse(401, ['error' => 'Invalid or expired token: ' . $e->getMessage()]);
            return null;
        }
    }

    /**
     * GET /api/products — List all products.
     */
    public function list(): void
    {
        $userId = $this->authenticate();
        if ($userId === null) return;

        $stmt = $this->db->query(
            'SELECT id, name, description, price, created_by, created_at FROM products ORDER BY created_at DESC'
        );

        $products = [];
        while ($row = $stmt->fetch()) {
            $row['id'] = (int) $row['id'];
            $row['price'] = (float) $row['price'];
            $products[] = $row;
        }

        $this->jsonResponse(200, ['products' => $products]);
    }

    /**
     * POST /api/products — Create a product.
     */
    public function create(): void
    {
        $userId = $this->authenticate();
        if ($userId === null) return;

        $input = json_decode(file_get_contents('php://input'), true);

        if (empty($input['name']) || !isset($input['price'])) {
            $this->jsonResponse(400, ['error' => 'name and price are required']);
            return;
        }

        $name = trim($input['name']);
        $description = trim($input['description'] ?? '');
        $price = (float) $input['price'];

        $stmt = $this->db->prepare(
            'INSERT INTO products (name, description, price, created_by) VALUES (:name, :desc, :price, :created_by) RETURNING id, created_at'
        );
        $stmt->execute([
            'name'       => $name,
            'desc'       => $description,
            'price'      => $price,
            'created_by' => $userId,
        ]);

        $result = $stmt->fetch();

        $this->jsonResponse(201, [
            'message' => 'Product created successfully',
            'product' => [
                'id'          => (int) $result['id'],
                'name'        => $name,
                'description' => $description,
                'price'       => $price,
                'created_by'  => $userId,
                'created_at'  => $result['created_at'],
            ],
        ]);
    }

    /**
     * GET /api/products?id=N — Get a product by ID.
     */
    public function get(): void
    {
        $userId = $this->authenticate();
        if ($userId === null) return;

        $id = (int) ($_GET['id'] ?? 0);
        if ($id <= 0) {
            $this->jsonResponse(404, ['error' => 'Valid product id is required']);
            return;
        }

        $stmt = $this->db->prepare(
            'SELECT id, name, description, price, created_by, created_at FROM products WHERE id = :id'
        );
        $stmt->execute(['id' => $id]);
        $product = $stmt->fetch();

        if (!$product) {
            $this->jsonResponse(404, ['error' => 'Product not found']);
            return;
        }

        $product['id'] = (int) $product['id'];
        $product['price'] = (float) $product['price'];

        $this->jsonResponse(200, ['product' => $product]);
    }

    private function jsonResponse(int $status, array $data): void
    {
        http_response_code($status);
        echo json_encode($data, JSON_UNESCAPED_UNICODE);
    }
}
