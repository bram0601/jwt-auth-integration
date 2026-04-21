package com.app.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Validates JWTs issued by the PHP Auth App using the shared RSA public key.
 */
public class JwtService {

    private final PublicKey publicKey;

    public JwtService() {
        this.publicKey = loadPublicKey();
    }

    /**
     * Validate the token and return its claims.
     *
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Load RSA public key from the classpath or file system.
     * First tries classpath (public.pem in resources), then falls back to
     * the configured path via system property "jwt.public.key.path".
     */
    private PublicKey loadPublicKey() {
        try {
            String pemContent = null;

            // Try classpath first
            InputStream is = getClass().getClassLoader().getResourceAsStream("public.pem");
            if (is != null) {
                pemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Fall back to system property
            if (pemContent == null) {
                String path = System.getProperty("jwt.public.key.path", "../../keys/public.pem");
                pemContent = java.nio.file.Files.readString(java.nio.file.Path.of(path));
            }

            // Parse PEM format
            String base64Key = pemContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA public key: " + e.getMessage(), e);
        }
    }
}
