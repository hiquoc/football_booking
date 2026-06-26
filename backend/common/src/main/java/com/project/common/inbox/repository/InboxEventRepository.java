package com.project.common.inbox.repository;

import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.entity.InboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    Optional<InboxEvent> findByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    @Query(value = """
            select *
            from inbox_events
            where status = 'RECEIVED'
              and next_retry_at <= current_timestamp
            order by received_at
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<InboxEvent> lockReceivedBatch(@Param("batchSize") int batchSize);

    long countByStatus(InboxEventStatus status);
}
