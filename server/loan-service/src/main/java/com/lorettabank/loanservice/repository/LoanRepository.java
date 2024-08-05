package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.LoanDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LoanRepository {
    private final JdbcTemplate jdbcTemplate;

    public LoanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LoanDTO createLoan(int userId, String loanType, double amount, String status) {
        String query = "INSERT INTO loans (user_id, loan_type, amount, status) VALUES (?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(query, new Object[]{userId, loanType, amount, status}, new LoanRowMapper());
    }

    public LoanDTO getLoan(int id, int userId) {
        String query = """
                SELECT l.*, u.*
                FROM loans l
                JOIN users u ON l.user_id = u.id
                WHERE l.id = ? AND l.user_id = ?""";
        return jdbcTemplate.queryForObject(query, new Object[]{id, userId}, new LoanRowMapper());
    }

    public int updateLoan(int id, int userId, String loanType, double amount, String status) {
        String query = """
                UPDATE loans
                SET loan_type = ?, amount = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? RETURNING *""";
        return jdbcTemplate.update(query, loanType, amount, status, id, userId);
    }

    public int deleteLoan(int id, int userId) {
        String query = "DELETE FROM loans WHERE id = ? AND user_id = ? RETURNING *";
        return jdbcTemplate.update(query, id, userId);
    }
}
