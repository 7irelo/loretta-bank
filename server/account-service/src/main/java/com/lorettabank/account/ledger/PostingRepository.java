package com.lorettabank.account.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostingRepository extends JpaRepository<PostingEntity, Long> {

    List<PostingEntity> findByJournalIdOrderByIdAsc(UUID journalId);

    Optional<PostingEntity> findFirstByAccountIdAndCreatedAtBeforeOrderByIdDesc(
            Long accountId, Instant before);

    @Query("select coalesce(sum(p.amount), 0) from PostingEntity p")
    BigDecimal sumAll();

    @Query(
            value =
                    """
                    select a.id from accounts a
                    left join (select account_id, sum(amount) as total from postings
                               where account_id is not null group by account_id) p
                      on p.account_id = a.id
                    where a.balance <> coalesce(p.total, 0)
                    """,
            nativeQuery = true)
    List<Long> findAccountsWithDrift();
}
