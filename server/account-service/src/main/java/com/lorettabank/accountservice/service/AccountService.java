package com.lorettabank.userservice.service;

import com.lorettabank.userservice.dto.AccountDTO;
import com.lorettabank.userservice.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountDTO createAccount(String userId, String accountType) {
        return accountRepository.createAccount(userId, accountType);
    }

    public List<AccountDTO> getAccounts(String userId) {
        return accountRepository.getAccounts(userId);
    }

    public AccountDTO getAccount(int id, String userId) {
        return accountRepository.getAccount(id, userId);
    }

    public int updateAccount(int id, String userId, String accountType, double balance, String accountStatus) {
        return accountRepository.updateAccount(id, userId, accountType, balance, accountStatus);
    }

    public int deleteAccount(int id, String userId) {
        return accountRepository.deleteAccount(id, userId);
    }
}
