<?php

namespace App;

use PDO;
use PDOException;

class Database
{
    private static ?PDO $instance = null;
    private static ?PDO $authInstance = null;

    /**
     * Get connection to the default database (app_db for products).
     */
    public static function getConnection(): PDO
    {
        if (self::$instance === null) {
            $config = require __DIR__ . '/../config/database.php';
            self::$instance = self::createConnection($config);
        }
        return self::$instance;
    }

    /**
     * Get connection to auth_db (for user operations).
     */
    public static function getAuthConnection(): PDO
    {
        if (self::$authInstance === null) {
            $config = require __DIR__ . '/../config/database.php';
            $config['dbname'] = getenv('AUTH_DB_NAME') ?: 'auth_db';
            self::$authInstance = self::createConnection($config);
        }
        return self::$authInstance;
    }

    private static function createConnection(array $config): PDO
    {
        $dsn = sprintf(
            'pgsql:host=%s;port=%s;dbname=%s',
            $config['host'],
            $config['port'],
            $config['dbname']
        );

        try {
            return new PDO($dsn, $config['username'], $config['password'], [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ]);
        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode(['error' => 'Database connection failed: ' . $e->getMessage()]);
            exit;
        }
    }
}
