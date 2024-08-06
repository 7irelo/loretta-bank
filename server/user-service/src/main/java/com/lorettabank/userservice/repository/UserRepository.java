package com.lorettabank.userservice.repository;

import com.lorettabank.userservice.dto.UserDTO;
import com.lorettabank.common.Account;
import com.lorettabank.common.Transaction;
import com.lorettabank.common.Loan;
import com.lorettabank.common.Card;
import com.lorettabank.userservice.mapper.UserMapper;
import com.lorettabank.userservice.mapper.AccountMapper;
import com.lorettabank.userservice.mapper.TransactionMapper;
import com.lorettabank.userservice.mapper.LoanMapper;
import com.lorettabank.userservice.mapper.CardMapper;
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

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private LoanMapper loanMapper;

    @Autowired
    private CardMapper cardMapper;

    private static final String INSERT_USER_SQL =
            "INSERT INTO users (id, first_name, last_name, email, date_of_birth, address, occupation, phone, username, password) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_USER_SQL =
            "UPDATE users SET first_name = ?, last_name = ?, email = ?, date_of_birth = ?, address = ?, occupation = ?, phone = ?, username = ?, password = ? " +
                    "WHERE id = ?";

    private static final String SELECT_USER_WITH_DETAILS_SQL =
            "SELECT u.id, u.first_name, u.last_name, u.email, u.date_of_birth, u.address, u.occupation, u.phone, u.username, u.password " +
                    "FROM users u WHERE u.id = ?";

    private static final String SELECT_USER_ACCOUNTS_SQL =
            "SELECT a.id, a.account_number, a.account_type, a.available_balance " +
                    "FROM accounts a WHERE a.user_id = ? ORDER BY a.created_at LIMIT 3";

    private static final String SELECT_ACCOUNT_TRANSACTIONS_SQL =
            "SELECT t.id, t.transaction_type, t.amount " +
                    "FROM transactions t WHERE t.account_id = ? ORDER BY t.date DESC LIMIT 3";

    private static final String SELECT_USER_LOANS_SQL =
            "SELECT l.id, l.loan_type, l.amount " +
                    "FROM loans l WHERE l.user_id = ? ORDER BY l.start_date DESC LIMIT 5";

    private static final String SELECT_USER_CARDS_SQL =
            "SELECT c.id, c.card_number, c.cvv " +
                    "FROM cards c WHERE c.user_id = ? ORDER BY c.created_at DESC LIMIT 5";

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
                    (rs, rowNum) -> {
                        UserDTO userDTO = new UserDTO();
                        userDTO.setId(rs.getString("id"));
                        return userDTO;
                    }
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<UserDTO> findByIdWithDetails(String userId) {
        try {
            UserDTO userDTO = jdbcTemplate.queryForObject(
                    SELECT_USER_WITH_DETAILS_SQL,
                    new Object[]{userId},
                    userMapper
            );

            List<Account> accounts = jdbcTemplate.query(SELECT_USER_ACCOUNTS_SQL, new Object[]{userId}, accountMapper);

            accounts.forEach(account -> {
                List<Transaction> transactions = jdbcTemplate.query(SELECT_ACCOUNT_TRANSACTIONS_SQL,
                        new Object[]{account.getId()}, transactionMapper);
                account.setTransactions(transactions);
            });

            userDTO.setAccounts(accounts);

            List<Loan> loans = jdbcTemplate.query(SELECT_USER_LOANS_SQL, new Object[]{userId}, loanMapper);
            userDTO.setLoans(loans);

            List<Card> cards = jdbcTemplate.query(SELECT_USER_CARDS_SQL, new Object[]{userId}, cardMapper);
            userDTO.setCards(cards);

            return Optional.of(userDTO);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
