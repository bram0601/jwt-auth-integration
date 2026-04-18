package com.app.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT Service")
class JwtServiceTest {

    private static JwtService jwtService;
    private static PrivateKey privateKey;

    @BeforeAll
    static void setUp() throws Exception {
        jwtService = new JwtService();

        // Load private key for test token generation
        // Try classpath first, then file system
        String pemContent = null;
        InputStream is = JwtServiceTest.class.getClassLoader().getResourceAsStream("private.pem");
        if (is != null) {
            pemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } else {
            // Fall back to file path relative to project
            pemContent = java.nio.file.Files.readString(
                java.nio.file.Path.of("../../keys/private.pem")
            );
        }

        String base64Key = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        privateKey = keyFactory.generatePrivate(spec);
    }

    /**
     * Helper: create a valid JWT signed with the shared private key.
     */
    private String createTestToken(String sub, String email, String name, Date exp) {
        return Jwts.builder()
                .issuer("php-auth-app")
                .subject(sub)
                .claim("email", email)
                .claim("name", name)
                .issuedAt(new Date())
                .expiration(exp)
                .signWith(privateKey)
                .compact();
    }

    @Test
    @DisplayName("Validates a correctly signed token")
    void validateValidToken() {
        Date exp = new Date(System.currentTimeMillis() + 3600_000);
        String token = createTestToken("42", "test@example.com", "Test User", exp);

        Claims claims = jwtService.validateToken(token);

        assertEquals("42", claims.getSubject());
        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals("Test User", claims.get("name", String.class));
        assertEquals("php-auth-app", claims.getIssuer());
    }

    @Test
    @DisplayName("Extracts all expected claims")
    void extractsAllClaims() {
        Date exp = new Date(System.currentTimeMillis() + 3600_000);
        String token = createTestToken("7", "alice@test.com", "Alice", exp);

        Claims claims = jwtService.validateToken(token);

        assertNotNull(claims.getSubject());
        assertNotNull(claims.getIssuer());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.get("email"));
        assertNotNull(claims.get("name"));
    }

    @Test
    @DisplayName("Rejects expired token")
    void rejectsExpiredToken() {
        Date expired = new Date(System.currentTimeMillis() - 1000);
        String token = createTestToken("1", "test@example.com", "Test", expired);

        assertThrows(Exception.class, () -> jwtService.validateToken(token));
    }

    @Test
    @DisplayName("Rejects token with wrong issuer")
    void rejectsWrongIssuer() {
        Date exp = new Date(System.currentTimeMillis() + 3600_000);
        String token = Jwts.builder()
                .issuer("wrong-issuer")
                .subject("1")
                .expiration(exp)
                .signWith(privateKey)
                .compact();

        assertThrows(Exception.class, () -> jwtService.validateToken(token));
    }

    @Test
    @DisplayName("Rejects completely invalid token string")
    void rejectsGarbageToken() {
        assertThrows(Exception.class, () -> jwtService.validateToken("not.a.jwt"));
    }

    @Test
    @DisplayName("Rejects tampered token")
    void rejectsTamperedToken() {
        Date exp = new Date(System.currentTimeMillis() + 3600_000);
        String token = createTestToken("1", "test@example.com", "Test", exp);

        // Tamper with payload
        String[] parts = token.split("\\.");
        parts[1] = parts[1] + "TAMPERED";
        String tampered = String.join(".", parts);

        assertThrows(Exception.class, () -> jwtService.validateToken(tampered));
    }

    @Test
    @DisplayName("Rejects empty token")
    void rejectsEmptyToken() {
        assertThrows(Exception.class, () -> jwtService.validateToken(""));
    }
}
