package com.enterprise.inventory.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a product in the catalog.
 * Uses MongoDB for flexible schema — different product types have different attributes.
 *
 * Example: A laptop has "ram", "cpu", "storage" fields,
 * while a clothing item has "size", "color", "material" fields.
 * MongoDB handles this naturally with its document model.
 */
@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;

    private String sku;

    private String name;

    private String description;

    private String category;

    /** Product type determines which attributes are relevant */
    private ProductType productType;

    private Double price;

    private String currency;

    /** Flexible attributes — different per product type */
    private Map<String, Object> attributes;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) active = true;
        if (attributes == null) attributes = new HashMap<>();
    }

    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
