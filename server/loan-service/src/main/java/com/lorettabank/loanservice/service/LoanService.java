package com.lorettabank.userservice.service;

import com.lorettabank.userservice.dto.LoanDTO;
import com.lorettabank.userservice.repository.LoanRepository;
import org.springframework.stereotype.Service;

@Service
public class LoanService {
    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public LoanDTO createLoan(int userId, String loanType, double amount, String status) {
        return loanRepository.createLoan(userId, loanType, amount, status);
    }

    public LoanDTO getLoan(int id, int userId) {
        return loanRepository.getLoan(id, userId);
    }

    public int updateLoan(int id, int userId, String loanType, double amount, String status) {
        return loanRepository.updateLoan(id, userId, loanType, amount, status);
    }

    public int deleteLoan(int id, int userId) {
        return loanRepository.deleteLoan(id, userId);
    }
}
