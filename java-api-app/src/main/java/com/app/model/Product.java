package com.app.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Product {

    private int id;
    private String name;
    private String description;
    private BigDecimal price;
    private String createdBy; // user ID from JWT
    private Timestamp createdAt;

    public Product() {}

    public Product(String name, String description, BigDecimal price, String createdBy) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.createdBy = createdBy;
    }

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
