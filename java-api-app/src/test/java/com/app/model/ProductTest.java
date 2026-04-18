package com.app.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Model")
class ProductTest {

    @Test
    @DisplayName("Default constructor creates empty product")
    void defaultConstructor() {
        Product product = new Product();
        assertEquals(0, product.getId());
        assertNull(product.getName());
        assertNull(product.getDescription());
        assertNull(product.getPrice());
        assertNull(product.getCreatedBy());
        assertNull(product.getCreatedAt());
    }

    @Test
    @DisplayName("Parameterized constructor sets fields correctly")
    void parameterizedConstructor() {
        Product product = new Product("Laptop", "High-end", new BigDecimal("999.99"), "42");

        assertEquals("Laptop", product.getName());
        assertEquals("High-end", product.getDescription());
        assertEquals(new BigDecimal("999.99"), product.getPrice());
        assertEquals("42", product.getCreatedBy());
    }

    @Test
    @DisplayName("Setters and getters work correctly")
    void settersAndGetters() {
        Product product = new Product();
        Timestamp now = Timestamp.from(Instant.now());

        product.setId(1);
        product.setName("Widget");
        product.setDescription("A nice widget");
        product.setPrice(new BigDecimal("19.99"));
        product.setCreatedBy("5");
        product.setCreatedAt(now);

        assertEquals(1, product.getId());
        assertEquals("Widget", product.getName());
        assertEquals("A nice widget", product.getDescription());
        assertEquals(new BigDecimal("19.99"), product.getPrice());
        assertEquals("5", product.getCreatedBy());
        assertEquals(now, product.getCreatedAt());
    }

    @Test
    @DisplayName("Price handles decimal precision correctly")
    void priceDecimalPrecision() {
        Product product = new Product();
        product.setPrice(new BigDecimal("1299.50"));
        assertEquals(new BigDecimal("1299.50"), product.getPrice());

        product.setPrice(new BigDecimal("0.01"));
        assertEquals(new BigDecimal("0.01"), product.getPrice());
    }

    @Test
    @DisplayName("CreatedBy stores user ID as string")
    void createdByAsString() {
        Product product = new Product();
        product.setCreatedBy("123");
        assertEquals("123", product.getCreatedBy());
    }
}
