package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.TransactionDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransactionRepository {
    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TransactionDTO createTransaction(int accountId, String type, double amount, String description, String journalType) {
        String query = "INSERT INTO transactions (account_id, type, amount, description, journal_type) VALUES (?, ?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(query, new Object[]{accountId, type, amount, description, journalType}, new TransactionRowMapper());
    }

    public TransactionDTO getTransaction(int id, int userId) {
        String query = """
                SELECT t.*, a.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.id
                WHERE t.id = ? AND a.user_id = ?""";
        return jdbcTemplate.queryForObject(query, new Object[]{id, userId}, new TransactionRowMapper());
    }

    public List<TransactionDTO> getTransactions(int userId) {
        String query = """
                SELECT t.*, a.*
                FROM transactions t
                JOIN accounts a ON t.account_id = a.id
                WHERE a.user_id = ?""";
        return jdbcTemplate.query(query, new Object[]{userId}, new TransactionRowMapper());
    }

    public int deleteTransaction(int id, int userId) {
        String query = "DELETE FROM transactions WHERE id = ? AND account_id IN (SELECT id FROM accounts WHERE user_id = ?) RETURNING *";
        return jdbcTemplate.update(query, id, userId);
    }
}
