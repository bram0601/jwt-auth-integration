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

## API Usage

### Register a user (PHP App)

```bash
curl -X POST http://localhost:8080/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\": \"user@example.com\", \"password\": \"secret123\", \"name\": \"John Doe\"}"
```

### Login and get a JWT (PHP App)

```bash
curl -X POST http://localhost:8080/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\": \"user@example.com\", \"password\": \"secret123\"}"
```

Response:

```json
{
  "message": "Login successful",
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJS...",
  "user": { "id": 1, "email": "user@example.com", "name": "John Doe" }
}
```

### Get current user profile (PHP App)

```bash
curl http://localhost:8080/me -H "Authorization: Bearer YOUR_TOKEN"
```

### Create a product (Java App — requires JWT)

```bash
curl -X POST http://localhost:8081/java-api-app/api/products-create ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer YOUR_TOKEN" ^
  -d "{\"name\": \"Widget\", \"description\": \"A nice widget\", \"price\": 19.99}"
```

### List products (Java App — requires JWT)

```bash
curl http://localhost:8081/java-api-app/api/products ^
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Get a single product (Java App — requires JWT)

```bash
curl "http://localhost:8081/java-api-app/api/products-get?id=1" ^
  -H "Authorization: Bearer YOUR_TOKEN"
```

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
