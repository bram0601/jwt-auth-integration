# JWT Auth Integration — PHP + Java (Struts 2)

Cross-application authentication using JWT (RS256). The PHP app authenticates users and issues tokens; the Java Struts 2 app validates those tokens to protect its API endpoints.

## Architecture

```
[Client] --(register/login)--> [PHP Auth App :8080] --> [PostgreSQL: auth_db]
              |
              | JWT token (RS256)
              v
[Client] --(Bearer token)----> [Java API App :8081] --> [PostgreSQL: app_db]
```

**How it works:**
1. User registers and logs in via the **PHP Auth App**.
2. PHP app issues a JWT signed with an RSA private key (`RS256` algorithm).
3. User sends the JWT as a `Bearer` token to the **Java Struts 2 API App**.
4. Java app validates the JWT using the corresponding RSA public key via a Struts 2 interceptor.
5. If valid, the request proceeds; the user ID is extracted from the JWT `sub` claim.

**Components:**
- **PHP Auth App** — Pure PHP (no framework), Composer for `firebase/php-jwt` v7.x.
- **Java API App** — Apache Struts 2 (v6.3.x), `jjwt` for JWT validation, plain JDBC, packaged as a WAR.
- **Shared RSA key pair** — PHP signs with `keys/private.pem`, Java verifies with `keys/public.pem`.

## Prerequisites

- **PHP** >= 8.1 with `pdo_pgsql` extension
- **Composer** (PHP dependency manager)
- **Java JDK** 17+ (e.g. Eclipse Temurin)
- **Apache Maven** 3.9+
- **Apache Tomcat** 9.x (javax.servlet compatible)
- **PostgreSQL** 14+
- **OpenSSL** (for RSA key generation)

## Quick Start

### 1. Generate RSA Keys (if not already present)

```bash
mkdir keys
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/private.pem -out keys/public.pem
```

### 2. Database Setup

```bash
psql -U postgres -f db/init.sql
```

This creates both `auth_db` and `app_db` with their respective schemas.

### 3. PHP Auth App

```bash
cd php-auth-app
composer install
```

Start the built-in PHP dev server:

```bash
php -S localhost:8080 -t public
```

### 4. Java API App

Copy the public key into the Java app's classpath:

```bash
copy keys\public.pem java-api-app\src\main\resources\public.pem
```

Build the WAR:

```bash
cd java-api-app
mvn clean package
```

### 5. Deploy to Tomcat 9

1. Download and extract [Apache Tomcat 9.x](https://tomcat.apache.org/download-90.cgi).
2. Change Tomcat's HTTP port from `8080` to `8081` in `conf/server.xml` (to avoid conflict with the PHP app).
3. Copy the WAR to Tomcat's `webapps/` directory:
   ```bash
   copy java-api-app\target\java-api-app.war TOMCAT_HOME\webapps\
   ```
4. Set `JAVA_HOME` and start Tomcat:
   ```bash
   set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot"
   TOMCAT_HOME\bin\catalina.bat run
   ```

The Java app will be available at `http://localhost:8081/java-api-app/api/`.

## Authentication Flow

```
┌────────┐     1. POST /register          ┌───────────────┐     ┌───────────┐
│ Client │ ────────────────────▶ │ PHP Auth App  │ ───▶ │  auth_db  │
│        │     {email,password,name}  │ (port 8080)   │     │ (Postgres)│
└────────┘                            └───────────────┘     └───────────┘
    │                                    │
    │  2. POST /login                    │
    │     {email,password}               │
    │────────────────────────────────▶│
    │                                    │
    │  3. Response: {token: "eyJ..."}     │
    │◀────────────────────────────────│
    │
    │  4. GET /api/products              ┌───────────────┐     ┌───────────┐
    │     Authorization: Bearer eyJ...   │ Java API App │ ───▶ │  app_db   │
    │───────────────────────────────▶│ (port 8081)   │     │ (Postgres)│
    │                                   │               │     └───────────┘
    │  5. JWT validated via RSA public   │  Struts 2     │
    │     key → request authorized      │  Interceptor  │
    │                                   │  validates JWT│
    │  6. Response: {products: [...]}    │               │
    │◀───────────────────────────────│               │
                                        └───────────────┘
```

**Key points:**
- The PHP app is the **sole issuer** of JWT tokens (signs with RSA private key).
- The Java app **never** sees the private key — it only needs the public key to verify.
- Tokens are stateless; no session sharing or inter-service calls between the apps.
- The `created_by` field on products is automatically set from the JWT `sub` claim.

## API Reference

### PHP Auth App — `http://localhost:8080`

All responses are `Content-Type: application/json`.

---

#### `POST /register`

Create a new user account.

**Request:**
```bash
curl -X POST http://localhost:8080/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\": \"user@example.com\", \"password\": \"secret123\", \"name\": \"John Doe\"}"
```

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "secret123",
  "name": "John Doe"
}
```

**Success response** (`201 Created`):
```json
{
  "message": "User registered successfully",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe"
  }
}
```

**Error responses:**
- `400` — Missing required fields: `{"error": "email, password, and name are required"}`
- `409` — Duplicate email: `{"error": "Email already registered"}`

---

#### `POST /login`

Authenticate and receive a JWT token.

**Request:**
```bash
curl -X POST http://localhost:8080/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\": \"user@example.com\", \"password\": \"secret123\"}"
```

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

**Success response** (`200 OK`):
```json
{
  "message": "Login successful",
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe"
  }
}
```

The `token` field contains the JWT to use with both PHP and Java endpoints. It expires after **1 hour**.

**Error responses:**
- `400` — Missing fields: `{"error": "email and password are required"}`
- `401` — Bad credentials: `{"error": "Invalid email or password"}`

---

#### `GET /me`

Get the authenticated user's profile. Requires a valid JWT.

**Request:**
```bash
curl http://localhost:8080/me ^
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success response** (`200 OK`):
```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "created_at": "2026-04-18 11:40:51.131905"
  }
}
```

**Error responses:**
- `401` — Missing token: `{"error": "Authorization header with Bearer token required"}`
- `401` — Invalid/expired token: `{"error": "Invalid token: ..."}`
- `404` — User deleted: `{"error": "User not found"}`

---

### Java API App — `http://localhost:8081/java-api-app/api`

All endpoints require a valid JWT in the `Authorization: Bearer <token>` header. The token must have been issued by the PHP Auth App. All responses are `Content-Type: application/json`.

**Common error response** (`401 Unauthorized`) — returned by the JWT interceptor before the action executes:
```json
{"error": "Authorization header with Bearer token required"}
```
or:
```json
{"error": "Invalid or expired token: <details>"}
```

---

#### `GET /api/products`

List all products, ordered by creation date (newest first).

**Request:**
```bash
curl http://localhost:8081/java-api-app/api/products ^
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success response** (`200 OK`):
```json
{
  "products": [
    {
      "id": 2,
      "name": "Laptop",
      "description": "High-end laptop",
      "price": 999.99,
      "created_by": "1",
      "created_at": "2026-04-18 11:40:52.294426"
    },
    {
      "id": 1,
      "name": "Widget",
      "description": "A nice widget",
      "price": 19.99,
      "created_by": "1",
      "created_at": "2026-04-17 13:27:10.742341"
    }
  ]
}
```

The `created_by` field contains the user ID extracted from the JWT `sub` claim of the user who created the product.

---

#### `POST /api/products-create`

Create a new product. The `created_by` field is automatically set from the JWT.

**Request:**
```bash
curl -X POST http://localhost:8081/java-api-app/api/products-create ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer YOUR_TOKEN" ^
  -d "{\"name\": \"Widget\", \"description\": \"A nice widget\", \"price\": 19.99}"
```

**Request body:**
```json
{
  "name": "Widget",
  "description": "A nice widget",
  "price": 19.99
}
```

`name` and `price` are required. `description` is optional (defaults to empty string).

**Success response** (`201 Created`):
```json
{
  "message": "Product created successfully",
  "product": {
    "id": 1,
    "name": "Widget",
    "description": "A nice widget",
    "price": 19.99,
    "created_by": "1",
    "created_at": "2026-04-17 13:27:10.742341"
  }
}
```

**Error responses:**
- `400` — Missing fields: `{"error": "name and price are required"}`
- `500` — Database error: `{"error": "Database error: ..."}`

---

#### `GET /api/products-get?id={id}`

Get a single product by its ID.

**Request:**
```bash
curl "http://localhost:8081/java-api-app/api/products-get?id=1" ^
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success response** (`200 OK`):
```json
{
  "product": {
    "id": 1,
    "name": "Widget",
    "description": "A nice widget",
    "price": 19.99,
    "created_by": "1",
    "created_at": "2026-04-17 13:27:10.742341"
  }
}
```

**Error responses:**
- `404` — Not found: `{"error": "Product not found"}`
- `404` — Invalid ID: `{"error": "Valid product id is required"}`

---

### Database Schemas

**`auth_db.users`** (PHP Auth App):
- `id` — SERIAL PRIMARY KEY
- `email` — VARCHAR(255), UNIQUE, NOT NULL
- `password_hash` — VARCHAR(255), NOT NULL (bcrypt)
- `name` — VARCHAR(255), NOT NULL
- `created_at` — TIMESTAMP, defaults to now

**`app_db.products`** (Java API App):
- `id` — SERIAL PRIMARY KEY
- `name` — VARCHAR(255), NOT NULL
- `description` — TEXT, defaults to empty string
- `price` — DECIMAL(10,2), NOT NULL
- `created_by` — VARCHAR(50), NOT NULL (user ID from JWT `sub`)
- `created_at` — TIMESTAMP, defaults to now

## Project Structure

```
jwt-auth-integration/
├── keys/
│   ├── private.pem          # RSA private key (used by PHP to sign)
│   └── public.pem           # RSA public key (used by Java to verify)
├── db/
│   └── init.sql             # Creates both databases + schemas
├── php-auth-app/
│   ├── composer.json
│   ├── config/
│   │   ├── database.php
│   │   └── jwt.php
│   ├── public/
│   │   └── index.php        # Entry point + router
│   ├── src/
│   │   ├── AuthController.php
│   │   ├── Database.php
│   │   └── JwtService.php
│   └── db/
│       └── schema.sql
├── java-api-app/
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/app/
│   │   │   ├── action/ProductAction.java
│   │   │   ├── interceptor/JwtInterceptor.java
│   │   │   ├── model/Product.java
│   │   │   └── service/
│   │   │       ├── DatabaseService.java
│   │   │       └── JwtService.java
│   │   ├── resources/struts.xml
│   │   └── webapp/WEB-INF/web.xml
│   └── db/
│       └── schema.sql
└── README.md
```

## JWT Token Details

**Algorithm:** RS256 (RSA + SHA-256)

**Claims issued by PHP app:**
- `iss` — Issuer: `php-auth-app`
- `sub` — User ID (string)
- `email` — User email
- `name` — User display name
- `iat` — Issued-at timestamp
- `exp` — Expiry (1 hour after issue)

**Java validation:**
- Verifies RSA signature using the public key
- Requires `iss` claim to equal `php-auth-app`
- Rejects expired tokens
- Extracts `sub`, `email`, `name` from claims and injects into request attributes

## Configuration

### Environment Variables

**PHP App** (`php-auth-app`):
- `DB_HOST` — PostgreSQL host (default: `localhost`)
- `DB_PORT` — PostgreSQL port (default: `5432`)
- `DB_NAME` — Database name (default: `auth_db`)
- `DB_USER` — Database user (default: `postgres`)
- `DB_PASS` — Database password (default: `postgres`)
- `JWT_PRIVATE_KEY` — Path to RSA private key
- `JWT_PUBLIC_KEY` — Path to RSA public key

**Java App** (`java-api-app`):
- `DB_URL` — JDBC URL (default: `jdbc:postgresql://localhost:5432/app_db`)
- `DB_USER` — Database user (default: `postgres`)
- `DB_PASS` — Database password (default: `postgres`)
- System property `jwt.public.key.path` — Path to RSA public key (or place `public.pem` in classpath)

## Security Notes

- RSA keys in `keys/` are for **development only**. In production, use a secrets manager and rotate keys regularly.
- Passwords are hashed with **bcrypt** in the PHP app.
- JWT tokens expire after **1 hour**.
- The Java app validates the `iss` (issuer) claim matches `php-auth-app`.
- The asymmetric RSA approach means the Java app never needs access to the private key.
- All database queries use parameterized statements to prevent SQL injection.
