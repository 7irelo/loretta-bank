package com.lorettabank.accountservice.mapper;

import com.lorettabank.accountservice.repository.TransactionRepository;
import com.lorettabank.commonlibrary.model.Account;
import com.lorettabank.commonlibrary.model.Transaction;
import com.lorettabank.commonlibrary.model.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AccountRowMapper implements RowMapper<Account> {
    private final TransactionRepository transactionRepository;

    public AccountRowMapper(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
        // Map User object
        User user = User.builder()
                .id(rs.getString("user_id"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .build();

        // Get accountId to fetch transactions
        int accountId = rs.getInt("id");

        // Fetch the latest 15 transactions
        List<Transaction> transactions = transactionRepository.findLatest15ByAccountId(accountId);

        // Map Account object
        Account account = Account.builder()
                .id(accountId)
                .accountNumber(rs.getString("account_number"))
                .name(rs.getString("name"))
                .userId(rs.getString("user_id"))
                .accountType(rs.getString("account_type"))
                .availableBalance(rs.getDouble("available_balance"))
                .latestBalance(rs.getDouble("latest_balance"))
                .accountStatus(rs.getString("account_status"))
                .build();

        // Set the associated User and Transactions
        account.setUser(user);
        account.setTransactions(transactions);

        return account;
    }
}
