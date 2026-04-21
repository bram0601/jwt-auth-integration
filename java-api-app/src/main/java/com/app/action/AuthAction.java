package com.app.action;

import com.app.service.AuthDatabaseService;
import com.app.service.JwtSigningService;
import com.app.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensymphony.xwork2.ActionSupport;
import io.jsonwebtoken.Claims;
import org.apache.struts2.ServletActionContext;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.http.HttpServletRequest;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AuthAction extends ActionSupport {

    private Map<String, Object> responseData = new HashMap<>();
    private final JwtSigningService jwtSigning = new JwtSigningService();
    private final JwtService jwtValidation = new JwtService();

    public Map<String, Object> getResponseData() { return responseData; }

    /**
     * POST /auth/register
     */
    public String register() {
        HttpServletRequest request = ServletActionContext.getRequest();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> json = mapper.readValue(request.getInputStream(), Map.class);

            String email = (String) json.get("email");
            String password = (String) json.get("password");
            String name = (String) json.get("name");

            if (email == null || password == null || name == null ||
                email.isBlank() || password.isBlank() || name.isBlank()) {
                responseData.put("error", "email, password, and name are required");
                return ERROR;
            }

            email = email.trim();
            name = name.trim();

            try (Connection conn = AuthDatabaseService.getConnection()) {
                // Check duplicate
                PreparedStatement check = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
                check.setString(1, email);
                if (check.executeQuery().next()) {
                    responseData.put("error", "Email already registered");
                    return "conflict";
                }

                // Insert user
                String hash = BCrypt.hashpw(password, BCrypt.gensalt());
                PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO users (email, password_hash, name) VALUES (?, ?, ?) RETURNING id"
                );
                insert.setString(1, email);
                insert.setString(2, hash);
                insert.setString(3, name);
                ResultSet rs = insert.executeQuery();

                if (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("email", email);
                    user.put("name", name);
                    responseData.put("message", "User registered successfully");
                    responseData.put("user", user);
                    return SUCCESS;
                }
            }
            responseData.put("error", "Registration failed");
            return ERROR;

        } catch (Exception e) {
            responseData.put("error", "Server error: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * POST /auth/login
     */
    public String login() {
        HttpServletRequest request = ServletActionContext.getRequest();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> json = mapper.readValue(request.getInputStream(), Map.class);

            String email = (String) json.get("email");
            String password = (String) json.get("password");

            if (email == null || password == null || email.isBlank() || password.isBlank()) {
                responseData.put("error", "email and password are required");
                return ERROR;
            }

            try (Connection conn = AuthDatabaseService.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, email, password_hash, name FROM users WHERE email = ?"
                );
                stmt.setString(1, email.trim());
                ResultSet rs = stmt.executeQuery();

                if (rs.next() && BCrypt.checkpw(password, rs.getString("password_hash"))) {
                    int userId = rs.getInt("id");
                    String userEmail = rs.getString("email");
                    String userName = rs.getString("name");

                    String token = jwtSigning.generateToken(userId, userEmail, userName);

                    Map<String, Object> user = new HashMap<>();
                    user.put("id", userId);
                    user.put("email", userEmail);
                    user.put("name", userName);

                    responseData.put("message", "Login successful");
                    responseData.put("token", token);
                    responseData.put("user", user);
                    return SUCCESS;
                }
            }

            responseData.put("error", "Invalid email or password");
            return "unauthorized";

        } catch (Exception e) {
            responseData.put("error", "Server error: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * GET /auth/me — requires JWT in Authorization header
     */
    public String me() {
        HttpServletRequest request = ServletActionContext.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            responseData.put("error", "Authorization header with Bearer token required");
            return "unauthorized";
        }

        try {
            Claims claims = jwtValidation.validateToken(authHeader.substring(7));
            String userId = claims.getSubject();

            try (Connection conn = AuthDatabaseService.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, email, name, created_at FROM users WHERE id = ?"
                );
                stmt.setInt(1, Integer.parseInt(userId));
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("email", rs.getString("email"));
                    user.put("name", rs.getString("name"));
                    user.put("created_at", rs.getTimestamp("created_at").toString());
                    responseData.put("user", user);
                    return SUCCESS;
                }
            }

            responseData.put("error", "User not found");
            return "notFound";

        } catch (Exception e) {
            responseData.put("error", "Invalid or expired token: " + e.getMessage());
            return "unauthorized";
        }
    }
}
