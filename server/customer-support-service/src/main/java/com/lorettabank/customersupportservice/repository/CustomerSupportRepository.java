package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.CustomerSupportDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerSupportRepository {
    private final JdbcTemplate jdbcTemplate;

    public CustomerSupportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CustomerSupportDTO createSupport(int userId, String query, String response, String status) {
        String sql = "INSERT INTO customer_support (user_id, query, response, status) VALUES (?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(sql, new Object[]{userId, query, response, status}, new CustomerSupportRowMapper());
    }

    public CustomerSupportDTO getSupport(int id, int userId) {
        String sql = """
                SELECT cs.*, u.*
                FROM customer_support cs
                JOIN users u ON cs.user_id = u.id
                WHERE cs.id = ? AND cs.user_id = ?""";
        return jdbcTemplate.queryForObject(sql, new Object[]{id, userId}, new CustomerSupportRowMapper());
    }

    public int updateSupport(int id, int userId, String query, String response, String status) {
        String sql = """
                UPDATE customer_support
                SET query = ?, response = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? RETURNING *""";
        return jdbcTemplate.update(sql, query, response, status, id, userId);
    }

    public int deleteSupport(int id, int userId) {
        String sql = "DELETE FROM customer_support WHERE id = ? AND user_id = ? RETURNING *";
        return jdbcTemplate.update(sql, id, userId);
    }
}
