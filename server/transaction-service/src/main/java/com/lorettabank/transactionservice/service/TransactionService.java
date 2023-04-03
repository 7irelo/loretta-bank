package com.lorettabank.transactionservice.service;

import com.lorettabank.transactionservice.dto.TransactionDTO;
import com.lorettabank.transactionservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionDTO createTransaction(int accountId, String type, double amount, String description, String journalType) {
        return transactionRepository.createTransaction(accountId, type, amount, description, journalType);
    }

    public TransactionDTO getTransaction(int id, int userId) {
        return transactionRepository.getTransaction(id, userId);
    }

    public List<TransactionDTO> getTransactions(int userId) {
        return transactionRepository.getTransactions(userId);
    }

    public int deleteTransaction(int id, int userId) {
        return transactionRepository.deleteTransaction(id, userId);
    }
}
