package com.enterprise.inventory.repository;

import com.enterprise.inventory.domain.InventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryItemRepository extends MongoRepository<InventoryItem, String> {

    Optional<InventoryItem> findByProductIdAndWarehouseId(String productId, String warehouseId);

    Optional<InventoryItem> findByProductId(String productId);
}
