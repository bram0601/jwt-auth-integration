package com.app.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC connection helper for PostgreSQL auth_db (users).
 */
public class AuthDatabaseService {

    private static final String DEFAULT_URL  = "jdbc:postgresql://localhost:5432/auth_db";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASS = "postgres";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url  = System.getProperty("auth.db.url", System.getenv("AUTH_DB_URL") != null ? System.getenv("AUTH_DB_URL") : DEFAULT_URL);
        String user = System.getProperty("auth.db.user", System.getenv("AUTH_DB_USER") != null ? System.getenv("AUTH_DB_USER") : DEFAULT_USER);
        String pass = System.getProperty("auth.db.pass", System.getenv("AUTH_DB_PASS") != null ? System.getenv("AUTH_DB_PASS") : DEFAULT_PASS);

        return DriverManager.getConnection(url, user, pass);
    }
}
