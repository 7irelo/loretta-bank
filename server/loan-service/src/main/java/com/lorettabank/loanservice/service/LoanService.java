package com.lorettabank.loanservice.service;

import com.lorettabank.loanservice.dto.LoanDTO;
import com.lorettabank.loanservice.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanService {
    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public LoanDTO createLoan(int userId, int accountId, String loanType, double amount, double interestRate, int term, String startDate, String endDate, String status) {
        return loanRepository.createLoan(userId, accountId, loanType, amount, interestRate, term, startDate, endDate, status);
    }

    public LoanDTO getLoan(int id, int userId) {
        return loanRepository.getLoan(id, userId);
    }

    public List<LoanDTO> getLoans(int userId) {
        return loanRepository.getLoans(userId);
    }

    public int updateLoan(int id, int userId, String loanType, double amount, double interestRate, int term, String startDate, String endDate, String status) {
        return loanRepository.updateLoan(id, userId, loanType, amount, interestRate, term, startDate, endDate, status);
    }

    public int deleteLoan(int id, int userId) {
        return loanRepository.deleteLoan(id, userId);
    }
}
