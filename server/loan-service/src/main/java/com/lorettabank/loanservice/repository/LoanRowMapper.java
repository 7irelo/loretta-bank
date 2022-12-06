package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.LoanDTO;
import com.lorettabank.userservice.dto.UserDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LoanRowMapper implements RowMapper<LoanDTO> {
    @Override
    public LoanDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserDTO user = new UserDTO();
        user.setId(rs.getInt("user_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        user.setAddress(rs.getString("address"));
        user.setOccupation(rs.getString("occupation"));
        user.setPhone(rs.getString("phone"));
        user.setUsername(rs.getString("username"));

        LoanDTO loan = new LoanDTO();
        loan.setId(rs.getInt("id"));
        loan.setUserId(rs.getInt("user_id"));
        loan.setLoanType(rs.getString("loan_type"));
        loan.setAmount(rs.getDouble("amount"));
        loan.setStatus(rs.getString("status"));
        loan.setCreatedAt(rs.getString("created_at"));
        loan.setUpdatedAt(rs.getString("updated_at"));
        loan.setUser(user);

        return loan;
    }
}
