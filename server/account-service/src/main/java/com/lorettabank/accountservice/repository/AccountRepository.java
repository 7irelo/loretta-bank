package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.AccountDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AccountDTO createAccount(String userId, String accountType) {
        String query = "INSERT INTO accounts (user_id, account_type) VALUES (?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(query, new Object[]{userId, accountType}, new AccountRowMapper());
    }

    public List<AccountDTO> getAccounts(String userId) {
        String query = "SELECT a.*, u.* FROM accounts a JOIN users u ON a.user_id = u.id WHERE a.user_id = ?";
        return jdbcTemplate.query(query, new Object[]{userId}, new AccountRowMapper());
    }

    public AccountDTO getAccount(int id, String userId) {
        String query = "SELECT a.*, u.* FROM accounts a JOIN users u ON a.user_id = u.id WHERE a.id = ? AND a.user_id = ?";
        return jdbcTemplate.queryForObject(query, new Object[]{id, userId}, new AccountRowMapper());
    }

    public int updateAccount(int id, String userId, String accountType, double balance, String accountStatus) {
        String query = "UPDATE accounts SET account_type = ?, available_balance = ?, account_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?";
        return jdbcTemplate.update(query, accountType, balance, accountStatus, id, userId);
    }

    public int deleteAccount(int id, String userId) {
        String query = "DELETE FROM accounts WHERE id = ? AND user_id = ?";
        return jdbcTemplate.update(query, id, userId);
    }
}
