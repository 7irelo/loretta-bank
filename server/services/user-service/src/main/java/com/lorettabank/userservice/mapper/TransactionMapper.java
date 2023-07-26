package com.lorettabank.userservice.mapper;

import com.lorettabank.common.Transaction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TransactionMapper implements RowMapper<Transaction> {
    @Override
    public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getInt("id"));
        transaction.setTransactionType(rs.getString("transaction_type"));
        transaction.setAmount(rs.getDouble("amount"));
        return transaction;
    }
}
