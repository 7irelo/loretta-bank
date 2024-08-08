package com.lorettabank.userservice.controller;

import com.lorettabank.userservice.dto.LoanDTO;
import com.lorettabank.userservice.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<?> createLoan(@RequestBody LoanDTO loanDTO) {
        try {
            LoanDTO createdLoan = loanService.createLoan(
                    loanDTO.getUserId(),
                    loanDTO.getLoanType(),
                    loanDTO.getAmount(),
                    loanDTO.getStatus()
            );
            return ResponseEntity.status(201).body(createdLoan);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating loan");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoan(@PathVariable int id, @RequestParam("userId") int userId) {
        try {
            LoanDTO loan = loanService.getLoan(id, userId);
            if (loan == null) {
                return ResponseEntity.status(404).body("Loan not found");
            }
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching loan");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLoan(@PathVariable int id, @RequestBody LoanDTO loanDTO) {
        try {
            int result = loanService.updateLoan(id, loanDTO.getUserId(), loanDTO.getLoanType(), loanDTO.getAmount(), loanDTO.getStatus());
            if (result == 0) {
                return ResponseEntity.status(404).body("Loan not found");
            }
            return ResponseEntity.ok("Loan updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating loan");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLoan(@PathVariable int id, @RequestParam("userId") int userId) {
        try {
            int result = loanService.deleteLoan(id, userId);
            if (result == 0) {
                return ResponseEntity.status(404).body("Loan not found");
            }
            return ResponseEntity.ok("Loan deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting loan");
        }
    }
}
