package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.TransactionDTO;
import com.lorettabank.userservice.dto.AccountDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionRowMapper implements RowMapper<TransactionDTO> {
    @Override
    public TransactionDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        AccountDTO account = new AccountDTO();
        account.setId(rs.getInt("account_id"));
        account.setName(rs.getString("name"));
        account.setAccountType(rs.getString("account_type"));
        account.setAvailableBalance(rs.getDouble("available_balance"));
        account.setLatestBalance(rs.getDouble("latest_balance"));
        account.setAccountStatus(rs.getString("account_status"));
        account.setImageUrl(rs.getString("image_url"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setUserId(rs.getString("user_id"));

        TransactionDTO transaction = new TransactionDTO();
        transaction.setId(rs.getInt("id"));
        transaction.setAccountId(rs.getInt("account_id"));
        transaction.setType(rs.getString("type"));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setDescription(rs.getString("description"));
        transaction.setJournalType(rs.getString("journal_type"));
        transaction.setDate(rs.getString("date"));
        transaction.setAccount(account);

        return transaction;
    }
}
