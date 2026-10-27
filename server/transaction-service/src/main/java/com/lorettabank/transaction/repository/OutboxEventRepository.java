package com.lorettabank.transaction.repository;

import com.lorettabank.transaction.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(
            value =
                    "SELECT * FROM outbox_events WHERE published = false ORDER BY id"
                            + " LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> claimBatch(@Param("limit") int limit);

    @Modifying
    @Query(
            value =
                    "DELETE FROM outbox_events WHERE published = true"
                            + " AND created_at < NOW() - make_interval(days => :days)",
            nativeQuery = true)
    int deletePublishedOlderThanDays(@Param("days") int days);
}
