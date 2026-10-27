package com.lorettabank.transaction.repository;

import com.lorettabank.transaction.entity.Transaction;
import com.lorettabank.transaction.entity.TransactionType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {

    private static final char ESCAPE = '!';

    private TransactionSpecifications() {}

    public static Specification<Transaction> matching(
            Collection<Long> accountIds,
            TransactionType type,
            Instant from,
            Instant to,
            String text) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(
                    cb.or(
                            root.get("sourceAccountId").in(accountIds),
                            root.get("targetAccountId").in(accountIds)));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }
            if (text != null && !text.isBlank()) {
                String escaped =
                        text.toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
                String like = "%" + escaped + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("description")), like, ESCAPE),
                                cb.like(cb.lower(root.get("reference")), like, ESCAPE)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
