package com.lorettabank.accountservice.mapper;

import com.lorettabank.accountservice.dto.AccountDTO;
import com.lorettabank.accountservice.dto.UserDTO;
import com.lorettabank.accountservice.dto.TransactionDTO;
import com.lorettabank.accountservice.repository.TransactionRepository;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AccountRowMapper implements RowMapper<AccountDTO> {
    private final TransactionRepository transactionRepository;

    public AccountRowMapper(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

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

        int accountId = rs.getInt("id");

        List<TransactionDTO> transactions = transactionRepository.findLatest10ByAccountId(accountId);

        AccountDTO account = new AccountDTO();
        account.setId(accountId);
        account.setName(rs.getString("name"));
        account.setUserId(rs.getString("user_id"));
        account.setAccountType(rs.getString("account_type"));
        account.setAvailableBalance(rs.getDouble("available_balance"));
        account.setLatestBalance(rs.getDouble("latest_balance"));
        account.setAccountStatus(rs.getString("account_status"));
        account.setImageUrl(rs.getString("image_url"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setUser(user);
        account.setTransactions(transactions);

        return account;
    }
}
