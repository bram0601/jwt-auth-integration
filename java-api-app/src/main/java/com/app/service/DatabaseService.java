package com.app.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple JDBC connection helper for PostgreSQL (app_db).
 */
public class DatabaseService {

    private static final String DEFAULT_URL  = "jdbc:postgresql://localhost:5432/app_db";
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
        String url  = System.getProperty("db.url", System.getenv("DB_URL") != null ? System.getenv("DB_URL") : DEFAULT_URL);
        String user = System.getProperty("db.user", System.getenv("DB_USER") != null ? System.getenv("DB_USER") : DEFAULT_USER);
        String pass = System.getProperty("db.pass", System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : DEFAULT_PASS);

        return DriverManager.getConnection(url, user, pass);
    }
}
