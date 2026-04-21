package com.app.service;

import io.jsonwebtoken.Jwts;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Signs JWTs using the RSA private key. Used by the auth endpoints.
 */
public class JwtSigningService {

    private static final String ISSUER = "java-auth-app";
    private static final long EXPIRY_MS = 3600_000; // 1 hour

    private final PrivateKey privateKey;

    public JwtSigningService() {
        this.privateKey = loadPrivateKey();
    }

    public String generateToken(int userId, String email, String name) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("name", name)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRY_MS))
                .signWith(privateKey)
                .compact();
    }

    private PrivateKey loadPrivateKey() {
        try {
            String pemContent = null;

            InputStream is = getClass().getClassLoader().getResourceAsStream("private.pem");
            if (is != null) {
                pemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            if (pemContent == null) {
                String path = System.getProperty("jwt.private.key.path", "../../keys/private.pem");
                pemContent = java.nio.file.Files.readString(java.nio.file.Path.of(path));
            }

            String base64Key = pemContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA private key: " + e.getMessage(), e);
        }
    }
}
