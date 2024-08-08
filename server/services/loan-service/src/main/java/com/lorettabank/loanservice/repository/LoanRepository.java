package com.lorettabank.loanservice.repository;

import com.lorettabank.loanservice.dto.LoanDTO;
import com.lorettabank.loanservice.mapper.LoanRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LoanRepository {
    private final JdbcTemplate jdbcTemplate;

    public LoanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LoanDTO createLoan(int userId, int accountId, String loanType, double amount, double interestRate, int term, String startDate, String endDate, String status) {
        String query = "INSERT INTO loans (user_id, account_id, loan_type, amount, interest_rate, term, start_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(query, new Object[]{userId, accountId, loanType, amount, interestRate, term, startDate, endDate, status}, new LoanRowMapper());
    }

    public LoanDTO getLoan(int id, int userId) {
        String query = """
                SELECT l.*, u.*
                FROM loans l
                JOIN users u ON l.user_id = u.id
                WHERE l.id = ? AND l.user_id = ?""";
        return jdbcTemplate.queryForObject(query, new Object[]{id, userId}, new LoanRowMapper());
    }

    public List<LoanDTO> getLoans(int userId) {
        String query = """
                SELECT l.*, u.*
                FROM loans l
                JOIN users u ON l.user_id = u.id
                WHERE l.user_id = ?""";
        return jdbcTemplate.query(query, new Object[]{userId}, new LoanRowMapper());
    }

    public int updateLoan(int id, int userId, String loanType, double amount, double interestRate, int term, String startDate, String endDate, String status) {
        String query = """
                UPDATE loans
                SET loan_type = ?, amount = ?, interest_rate = ?, term = ?, start_date = ?, end_date = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? RETURNING *""";
        return jdbcTemplate.update(query, loanType, amount, interestRate, term, startDate, endDate, status, id, userId);
    }

    public int deleteLoan(int id, int userId) {
        String query = "DELETE FROM loans WHERE id = ? AND user_id = ? RETURNING *";
        return jdbcTemplate.update(query, id, userId);
    }
}
