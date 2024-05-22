package com.lorettabank.accountservice.repository;

import com.lorettabank.accountservice.mapper.AccountRowMapper;
import com.lorettabank.accountservice.dto.AccountDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionRepository transactionRepository;

    public AccountRepository(JdbcTemplate jdbcTemplate, TransactionRepository transactionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionRepository = transactionRepository;
    }

    public AccountDTO createAccount(AccountDTO accountDTO) {
        String query = "INSERT INTO accounts (user_id, account_type, name, available_balance, latest_balance, account_status, image_url, account_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
                query,
                new Object[]{
                        accountDTO.getUserId(),
                        accountDTO.getAccountType(),
                        accountDTO.getName(),
                        accountDTO.getAvailableBalance(),
                        accountDTO.getLatestBalance(),
                        accountDTO.getAccountStatus(),
                        accountDTO.getImageUrl(),
                        accountDTO.getAccountNumber()
                },
                new AccountRowMapper(transactionRepository)
        );
    }

    public List<AccountDTO> getAccounts(String userId) {
        String query = "SELECT a.*, u.first_name, u.last_name FROM accounts a JOIN users u ON a.user_id = u.id WHERE a.user_id = ?";
        return jdbcTemplate.query(query, new Object[]{userId}, new AccountRowMapper(transactionRepository));
    }

    public AccountDTO getAccount(int id, String userId) {
        String query = "SELECT a.*, u.first_name, u.last_name FROM accounts a JOIN users u ON a.user_id = u.id WHERE a.id = ? AND a.user_id = ?";
        return jdbcTemplate.queryForObject(query, new Object[]{id, userId}, new AccountRowMapper(transactionRepository));
    }

    public int updateAccount(int id, AccountDTO accountDTO) {
        String query = "UPDATE accounts SET account_type = ?, available_balance = ?, account_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?";
        return jdbcTemplate.update(query, accountDTO.getAccountType(), accountDTO.getAvailableBalance(), accountDTO.getAccountStatus(), id, accountDTO.getUserId());
    }

    public int deleteAccount(int id, String userId) {
        String query = "DELETE FROM accounts WHERE id = ? AND user_id = ?";
        return jdbcTemplate.update(query, id, userId);
    }
}
