package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.AccountDTO;
import com.lorettabank.userservice.dto.UserDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountRowMapper implements RowMapper<AccountDTO> {
    @Override
    public AccountDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserDTO user = new UserDTO();
        user.setId(rs.getString("user_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        user.setAddress(rs.getString("address"));
        user.setOccupation(rs.getString("occupation"));
        user.setPhone(rs.getString("phone"));
        user.setUsername(rs.getString("username"));

        AccountDTO account = new AccountDTO();
        account.setId(rs.getInt("id"));
        account.setName(rs.getString("name"));
        account.setUserId(rs.getString("user_id"));
        account.setAccountType(rs.getString("account_type"));
        account.setAvailableBalance(rs.getDouble("available_balance"));
        account.setLatestBalance(rs.getDouble("latest_balance"));
        account.setAccountStatus(rs.getString("account_status"));
        account.setImageUrl(rs.getString("image_url"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setUser(user);

        return account;
    }
}
