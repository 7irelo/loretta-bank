package com.lorettabank.accountservice.repository;

import com.lorettabank.accountservice.dto.TransactionDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransactionRepository {
    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TransactionDTO> findLatest10ByAccountId(int accountId) {
        String query = "SELECT * FROM transactions WHERE account_id = ? ORDER BY date DESC LIMIT 10";
        return jdbcTemplate.query(query, new Object[]{accountId}, new TransactionRowMapper());
    }
}
