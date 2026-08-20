package com.enterprise.inventory.config;

import com.enterprise.inventory.domain.InventoryItem;
import com.enterprise.inventory.domain.Product;
import com.enterprise.inventory.domain.ProductType;
import com.enterprise.inventory.repository.InventoryItemRepository;
import com.enterprise.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Seeds the product catalog and stock levels so the Saga can actually
 * reserve inventory. Runs only when the collections are empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public void run(String... args) {
        seedProduct("PROD-001", "LAPTOP-001", "Laptop", "Electronics", ProductType.ELECTRONICS,
                4999.99, "BRL", Map.of("ram", "16GB", "cpu", "i7", "storage", "512GB SSD"), 50);
        seedProduct("PROD-002", "MOUSE-001", "Wireless Mouse", "Electronics", ProductType.ELECTRONICS,
                129.90, "BRL", Map.of("type", "bluetooth"), 200);
        seedProduct("PROD-003", "KEYBRD-001", "Mechanical Keyboard", "Electronics", ProductType.ELECTRONICS,
                459.00, "BRL", Map.of("switch", "red"), 100);
        seedProduct("PROD-004", "MON-001", "27in 4K Monitor", "Electronics", ProductType.ELECTRONICS,
                2499.00, "BRL", Map.of("panel", "IPS", "resolution", "4K"), 40);
        seedProduct("PROD-005", "HEAD-001", "Noise-Cancelling Headphones", "Electronics", ProductType.ELECTRONICS,
                899.90, "BRL", Map.of("type", "over-ear"), 60);
        log.info("Inventory seed completed");
    }

    private void seedProduct(String id, String sku, String name, String category,
                             ProductType type, double price, String currency,
                             Map<String, Object> attributes, int stock) {
        if (inventoryItemRepository.findByProductId(id).isPresent()) {
            return;
        }

        Product product = Product.builder()
                .id(id)
                .sku(sku)
                .name(name)
                .description(name)
                .category(category)
                .productType(type)
                .price(price)
                .currency(currency)
                .attributes(attributes)
                .active(true)
                .build();
        product.prePersist();
        productRepository.save(product);

        InventoryItem item = InventoryItem.builder()
                .productId(id)
                .warehouseId("WH-01")
                .totalQuantity(stock)
                .reservedQuantity(0)
                .build();
        item.prePersist();
        inventoryItemRepository.save(item);

        log.info("Seeded product {} ({}) with stock {}", id, name, stock);
    }
}
