package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.CustomerSupportDTO;
import com.lorettabank.userservice.dto.UserDTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerSupportRowMapper implements RowMapper<CustomerSupportDTO> {
    @Override
    public CustomerSupportDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
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

        CustomerSupportDTO support = new CustomerSupportDTO();
        support.setId(rs.getInt("id"));
        support.setUserId(rs.getInt("user_id"));
        support.setQuery(rs.getString("query"));
        support.setResponse(rs.getString("response"));
        support.setStatus(rs.getString("status"));
        support.setCreatedAt(rs.getString("created_at"));
        support.setUpdatedAt(rs.getString("updated_at"));
        support.setUser(user);

        return support;
    }
}
