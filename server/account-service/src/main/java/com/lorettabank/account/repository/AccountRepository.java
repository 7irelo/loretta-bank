package com.lorettabank.account.repository;

import com.lorettabank.account.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByAccountNumber(String accountNumber);

    List<AccountEntity> findByCustomerId(Long customerId);

    boolean existsByAccountNumber(String accountNumber);

    Page<AccountEntity> findAllByOrderByIdDesc(Pageable pageable);

    /** Locks rows in id order so concurrent journals touching the same accounts cannot deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.id in :ids order by a.id")
    List<AccountEntity> lockAllById(@Param("ids") Collection<Long> ids);
}
