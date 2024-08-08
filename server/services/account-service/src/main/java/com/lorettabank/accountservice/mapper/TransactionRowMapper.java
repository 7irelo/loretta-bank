package com.lorettabank.accountservice.mapper;

import com.lorettabank.accountservice.dto.TransactionDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionRowMapper implements RowMapper<TransactionDTO> {
    @Override
    public TransactionDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TransactionDTO(
            rs.getInt("id"),
            rs.getInt("account_id"),
            rs.getString("transaction_type"),
            rs.getDouble("amount"),
            rs.getTimestamp("date").toLocalDateTime(),
            rs.getString("description"),
            rs.getString("journal_type")
        );
    }
}
