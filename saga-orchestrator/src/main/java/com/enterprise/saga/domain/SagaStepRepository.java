package com.enterprise.saga.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {

    Optional<SagaStep> findByStepId(String stepId);

    List<SagaStep> findBySagaInstanceAndType(SagaInstance sagaInstance, StepType type);

    @Query("SELECT ss FROM SagaStep ss WHERE ss.sagaInstance.sagaId = :sagaId ORDER BY ss.stepOrder ASC")
    List<SagaStep> findBySagaIdOrdered(@Param("sagaId") String sagaId);

    @Query("SELECT ss FROM SagaStep ss WHERE ss.sagaInstance.id = :sagaId AND ss.status = :status")
    List<SagaStep> findBySagaIdAndStatus(@Param("sagaId") Long sagaId, @Param("status") StepStatus status);

    @Query("SELECT ss FROM SagaStep ss WHERE ss.sagaInstance.sagaId = :sagaId AND ss.stepOrder = :stepOrder")
    Optional<SagaStep> findBySagaIdAndStepOrder(@Param("sagaId") String sagaId, @Param("stepOrder") Integer stepOrder);
}
