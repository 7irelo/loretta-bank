package com.lorettabank.userservice.mapper;

import com.lorettabank.common.User;
import com.lorettabank.userservice.dto.UserDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class UserMapper implements RowMapper<UserDTO> {
    @Override
    public UserDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(rs.getString("id"));
        userDTO.setFirstName(rs.getString("first_name"));
        userDTO.setLastName(rs.getString("last_name"));
        userDTO.setEmail(rs.getString("email"));
        userDTO.setDateOfBirth(rs.getString("date_of_birth"));
        userDTO.setAddress(rs.getString("address"));
        userDTO.setOccupation(rs.getString("occupation"));
        userDTO.setPhone(rs.getString("phone"));
        userDTO.setUsername(rs.getString("username"));
        userDTO.setPassword(rs.getString("password"));
        return userDTO;
    }
}
