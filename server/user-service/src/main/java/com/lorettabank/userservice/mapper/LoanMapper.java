package com.lorettabank.userservice.mapper;

import com.lorettabank.common.Loan;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class LoanMapper implements RowMapper<Loan> {
    @Override
    public Loan mapRow(ResultSet rs, int rowNum) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getInt("id"));
        loan.setLoanType(rs.getString("loan_type"));
        loan.setAmount(rs.getDouble("amount"));
        return loan;
    }
}
