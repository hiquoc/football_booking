package com.project.common.outbox.repository;

import com.project.common.outbox.entity.OutboxEvent;
import com.project.common.outbox.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            select *
            from outbox_events
            where status = 'PENDING'
              and next_retry_at <= current_timestamp
            order by created_at
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> lockPendingBatch(@Param("batchSize") int batchSize);

    long countByStatus(OutboxEventStatus status);
}
