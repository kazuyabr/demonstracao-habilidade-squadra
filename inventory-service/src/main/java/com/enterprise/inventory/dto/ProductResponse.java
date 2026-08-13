package com.enterprise.inventory.dto;

import com.enterprise.inventory.domain.ProductType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private String id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private ProductType productType;
    private Double price;
    private String currency;
    private Map<String, Object> attributes;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
