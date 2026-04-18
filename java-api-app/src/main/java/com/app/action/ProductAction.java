package com.app.action;

import com.app.model.Product;
import com.app.service.DatabaseService;
import com.opensymphony.xwork2.ActionSupport;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts2.ServletActionContext;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAction extends ActionSupport {

    // Input fields (set by Struts parameter interceptor for create)
    private String name;
    private String description;
    private BigDecimal price;

    // For single product lookup
    private int id;

    // Output
    private Map<String, Object> responseData = new HashMap<>();

    // --- Getters/Setters for Struts ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Map<String, Object> getResponseData() { return responseData; }

    // --- Actions ---

    /**
     * GET /api/products — List all products.
     */
    public String list() {
        try (Connection conn = DatabaseService.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, description, price, created_by, created_at FROM products ORDER BY created_at DESC"
            );
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> products = new ArrayList<>();
            while (rs.next()) {
                products.add(productRowToMap(rs));
            }

            responseData.put("products", products);
            return SUCCESS;

        } catch (SQLException e) {
            responseData.put("error", "Database error: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * POST /api/products-create — Create a new product.
     */
    public String create() {
        HttpServletRequest request = ServletActionContext.getRequest();

        // Read JSON body manually since Struts params interceptor may not parse JSON
        try {
            String body = new String(request.getInputStream().readAllBytes());
            if (body != null && !body.isEmpty()) {
                // Simple JSON parsing (avoiding extra dependencies)
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> json = mapper.readValue(body, Map.class);
                if (json.containsKey("name")) this.name = (String) json.get("name");
                if (json.containsKey("description")) this.description = (String) json.get("description");
                if (json.containsKey("price")) this.price = new BigDecimal(json.get("price").toString());
            }
        } catch (Exception e) {
            // fall through to validation
        }

        if (name == null || name.isBlank() || price == null) {
            responseData.put("error", "name and price are required");
            return ERROR;
        }

        String userId = (String) request.getAttribute("userId");

        try (Connection conn = DatabaseService.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO products (name, description, price, created_by) VALUES (?, ?, ?, ?) RETURNING id, created_at"
            );
            stmt.setString(1, name);
            stmt.setString(2, description != null ? description : "");
            stmt.setBigDecimal(3, price);
            stmt.setString(4, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", rs.getInt("id"));
                product.put("name", name);
                product.put("description", description);
                product.put("price", price);
                product.put("created_by", userId);
                product.put("created_at", rs.getTimestamp("created_at").toString());

                responseData.put("message", "Product created successfully");
                responseData.put("product", product);
            }

            return SUCCESS;

        } catch (SQLException e) {
            responseData.put("error", "Database error: " + e.getMessage());
            return ERROR;
        }
    }

    /**
     * GET /api/products-get?id=N — Get a product by ID.
     */
    public String get() {
        if (id <= 0) {
            responseData.put("error", "Valid product id is required");
            return "notFound";
        }

        try (Connection conn = DatabaseService.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, description, price, created_by, created_at FROM products WHERE id = ?"
            );
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                responseData.put("product", productRowToMap(rs));
                return SUCCESS;
            } else {
                responseData.put("error", "Product not found");
                return "notFound";
            }

        } catch (SQLException e) {
            responseData.put("error", "Database error: " + e.getMessage());
            return ERROR;
        }
    }

    private Map<String, Object> productRowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rs.getInt("id"));
        map.put("name", rs.getString("name"));
        map.put("description", rs.getString("description"));
        map.put("price", rs.getBigDecimal("price"));
        map.put("created_by", rs.getString("created_by"));
        map.put("created_at", rs.getTimestamp("created_at").toString());
        return map;
    }
}
