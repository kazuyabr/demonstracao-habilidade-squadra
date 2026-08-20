package com.enterprise.inventory.repository;

import com.enterprise.inventory.domain.ReservationStatus;
import com.enterprise.inventory.domain.StockReservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends MongoRepository<StockReservation, String> {

    Optional<StockReservation> findByOrderIdAndProductId(String orderId, String productId);

    List<StockReservation> findAllByOrderIdAndProductId(String orderId, String productId);

    List<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByStatus(ReservationStatus status);
}
