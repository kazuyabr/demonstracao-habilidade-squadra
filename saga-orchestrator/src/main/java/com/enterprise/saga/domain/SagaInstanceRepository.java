package com.enterprise.saga.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, Long> {

    Optional<SagaInstance> findBySagaId(String sagaId);

    Optional<SagaInstance> findByOrderId(String orderId);

    List<SagaInstance> findByStatus(SagaStatus status);

    List<SagaInstance> findBySagaType(String sagaType);

    @Query("SELECT s FROM SagaInstance s WHERE s.status IN ('CREATED', 'IN_PROGRESS') " +
            "AND s.createdAt < :timeout")
    List<SagaInstance> findTimedOutSagas(@Param("timeout") java.time.LocalDateTime timeout);

    @Query("SELECT COUNT(s) FROM SagaInstance s WHERE s.status = :status")
    long countByStatus(@Param("status") SagaStatus status);
}
