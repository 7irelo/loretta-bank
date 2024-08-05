package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public UserDTO save(UserDTO userDTO) {
        String sql = "INSERT INTO users (id, first_name, last_name, email, date_of_birth, address, occupation, phone, username, password) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *";

        return jdbcTemplate.queryForObject(sql, new Object[]{
                userDTO.getId(),
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getEmail(),
                userDTO.getDateOfBirth(),
                userDTO.getAddress(),
                userDTO.getOccupation(),
                userDTO.getPhone(),
                userDTO.getUsername(),
                userDTO.getPassword()
        }, this::mapRowToUserDTO);
    }

    public Optional<UserDTO> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        return jdbcTemplate.query(sql, new Object[]{username}, this::mapRowToUserDTO)
                .stream()
                .findFirst();
    }

    public Optional<UserDTO> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.query(sql, new Object[]{id}, this::mapRowToUserDTO)
                .stream()
                .findFirst();
    }

    public UserDTO update(UserDTO userDTO) {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, date_of_birth = ?, address = ?, occupation = ?, phone = ?, username = ?, password = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? RETURNING *";
        return jdbcTemplate.queryForObject(sql, new Object[]{
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getEmail(),
                userDTO.getDateOfBirth(),
                userDTO.getAddress(),
                userDTO.getOccupation(),
                userDTO.getPhone(),
                userDTO.getUsername(),
                userDTO.getPassword(),
                userDTO.getId()
        }, this::mapRowToUserDTO);
    }

    private UserDTO mapRowToUserDTO(ResultSet rs, int rowNum) throws SQLException {
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
