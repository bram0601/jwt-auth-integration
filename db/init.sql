-- ============================================================
-- Database initialization script
-- Run as a PostgreSQL superuser (e.g. postgres)
-- Usage: psql -U postgres -f init.sql
-- ============================================================

-- Create auth_db for the PHP Auth Application
CREATE DATABASE auth_db;

-- Create app_db for the Java API Application
CREATE DATABASE app_db;

-- Connect to auth_db and create schema
\c auth_db

CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- Connect to app_db and create schema
\c app_db

CREATE TABLE IF NOT EXISTS products (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT DEFAULT '',
    price       DECIMAL(10, 2) NOT NULL,
    created_by  VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_created_by ON products (created_by);
