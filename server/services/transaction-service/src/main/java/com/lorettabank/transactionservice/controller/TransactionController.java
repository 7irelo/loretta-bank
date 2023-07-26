package com.lorettabank.userservice.controller;

import com.lorettabank.userservice.dto.TransactionDTO;
import com.lorettabank.userservice.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody TransactionDTO transactionDTO) {
        try {
            TransactionDTO createdTransaction = transactionService.createTransaction(
                    transactionDTO.getAccountId(),
                    transactionDTO.getType(),
                    transactionDTO.getAmount(),
                    transactionDTO.getDescription(),
                    transactionDTO.getJournalType()
            );
            return ResponseEntity.status(201).body(createdTransaction);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating transaction");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable int id, @RequestParam("userId") int userId) {
        try {
            TransactionDTO transaction = transactionService.getTransaction(id, userId);
            if (transaction == null) {
                return ResponseEntity.status(404).body("Transaction not found");
            }
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching transaction");
        }
    }

    @GetMapping
    public ResponseEntity<?> getTransactions(@RequestParam("userId") int userId) {
        try {
            List<TransactionDTO> transactions = transactionService.getTransactions(userId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching transactions");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable int id, @RequestParam("userId") int userId) {
        try {
            int result = transactionService.deleteTransaction(id, userId);
            if (result == 0) {
                return ResponseEntity.status(404).body("Transaction not found");
            }
            return ResponseEntity.ok("Transaction deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting transaction");
        }
    }
}
