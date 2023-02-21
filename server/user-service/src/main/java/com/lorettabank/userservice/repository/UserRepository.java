package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.UserDTO;
import com.lorettabank.userservice.dto.AccountDTO;
import com.lorettabank.userservice.dto.TransactionDTO;
import com.lorettabank.userservice.dto.LoanDTO;
import com.lorettabank.userservice.dto.CardDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String INSERT_USER_SQL =
        "INSERT INTO users (id, first_name, last_name, email, date_of_birth, address, occupation, phone, username, password) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_USER_SQL =
        "UPDATE users SET first_name = ?, last_name = ?, email = ?, date_of_birth = ?, address = ?, occupation = ?, phone = ?, username = ?, password = ? " +
        "WHERE id = ?";

    private static final String SELECT_USER_WITH_DETAILS_SQL =
        "SELECT " +
        "  u.id, u.first_name, u.last_name, u.email, u.date_of_birth, u.address, u.occupation, u.phone, u.username, u.password, " +
        "  COALESCE(jsonb_agg(DISTINCT jsonb_build_object('name', a.name, 'accountNumber', a.account_number, 'availableBalance', a.available_balance)) FILTER (WHERE a.id IS NOT NULL), '[]') AS accounts, " +
        "  COALESCE(jsonb_agg(DISTINCT jsonb_build_object('transactionType', t.transaction_type, 'amount', t.amount)) FILTER (WHERE t.id IS NOT NULL), '[]') AS transactions, " +
        "  COALESCE(jsonb_agg(DISTINCT jsonb_build_object('loanType', l.loan_type, 'amount', l.amount)) FILTER (WHERE l.id IS NOT NULL), '[]') AS loans, " +
        "  COALESCE(jsonb_agg(DISTINCT jsonb_build_object('cardNumber', c.card_number, 'cvv', c.cvv)) FILTER (WHERE c.id IS NOT NULL), '[]') AS cards " +
        "FROM users u " +
        "LEFT JOIN accounts a ON u.id = a.user_id " +
        "LEFT JOIN ( " +
        "  SELECT * FROM transactions WHERE id IN ( " +
        "    SELECT id FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE user_id = ?) " +
        "    ORDER BY date DESC LIMIT 5 " +
        "  ) " +
        ") t ON a.id = t.account_id " +
        "LEFT JOIN ( " +
        "  SELECT * FROM loans WHERE id IN ( " +
        "    SELECT id FROM loans WHERE user_id = ? " +
        "    ORDER BY start_date DESC LIMIT 5 " +
        "  ) " +
        ") l ON u.id = l.user_id " +
        "LEFT JOIN ( " +
        "  SELECT * FROM cards WHERE id IN ( " +
        "    SELECT id FROM cards WHERE user_id = ? " +
        "    ORDER BY created_at DESC LIMIT 5 " +
        "  ) " +
        ") c ON u.id = c.user_id " +
        "WHERE u.id = ? " +
        "GROUP BY u.id";

    public void save(UserDTO userDTO) {
        Optional<UserDTO> existingUser = findById(userDTO.getId());
        if (existingUser.isPresent()) {
            // Update existing user
            jdbcTemplate.update(UPDATE_USER_SQL,
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getEmail(),
                java.sql.Date.valueOf(userDTO.getDateOfBirth()), // Assuming date_of_birth is in yyyy-mm-dd format
                userDTO.getAddress(),
                userDTO.getOccupation(),
                userDTO.getPhone(),
                userDTO.getUsername(),
                userDTO.getPassword(),
                userDTO.getId()
            );
        } else {
            // Create new user
            jdbcTemplate.update(INSERT_USER_SQL,
                userDTO.getId(),
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getEmail(),
                java.sql.Date.valueOf(userDTO.getDateOfBirth()), // Assuming date_of_birth is in yyyy-mm-dd format
                userDTO.getAddress(),
                userDTO.getOccupation(),
                userDTO.getPhone(),
                userDTO.getUsername(),
                userDTO.getPassword()
            );
        }
    }

    private Optional<UserDTO> findById(String id) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE id = ?",
                new Object[]{id},
                (rs, rowNum) -> new UserDTO() // Placeholder mapping, as we only need to check existence
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private final RowMapper<UserDTO> userWithDetailsRowMapper = new RowMapper<UserDTO>() {
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

            // Parsing JSON arrays into lists
            userDTO.setAccounts(parseJsonArray(rs.getString("accounts"), AccountDTO.class));
            userDTO.setTransactions(parseJsonArray(rs.getString("transactions"), TransactionDTO.class));
            userDTO.setLoans(parseJsonArray(rs.getString("loans"), LoanDTO.class));
            userDTO.setCards(parseJsonArray(rs.getString("cards"), CardDTO.class));

            return userDTO;
        }
    };

    public Optional<UserDTO> findByIdWithDetails(String userId) {
        try {
            UserDTO userDTO = jdbcTemplate.queryForObject(
                SELECT_USER_WITH_DETAILS_SQL,
                new Object[]{userId, userId, userId, userId},
                userWithDetailsRowMapper
            );
            return Optional.of(userDTO);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private <T> List<T> parseJsonArray(String jsonArray, Class<T> clazz) {
        try {
            return jdbcTemplate.getObjectMapper().readValue(jsonArray, jdbcTemplate.getObjectMapper().getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            // Handle parsing exception
            throw new RuntimeException("Failed to parse JSON array", e);
        }
    }
}
