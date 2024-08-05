package com.lorettabank.userservice.controller;

import com.lorettabank.userservice.dto.AccountDTO;
import com.lorettabank.userservice.serializer.AccountSerializer;
import com.lorettabank.userservice.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody AccountDTO accountDTO) {
        try {
            AccountDTO createdAccount = accountService.createAccount(accountDTO.getUser().getId(), accountDTO.getAccountType());
            return ResponseEntity.status(201).body(AccountSerializer.serialize(createdAccount));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating account");
        }
    }

    @GetMapping
    public ResponseEntity<?> getAccounts(@RequestParam("userId") String userId) {
        try {
            List<AccountDTO> accounts = accountService.getAccounts(userId);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching accounts");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAccount(@PathVariable int id, @RequestParam("userId") String userId) {
        try {
            AccountDTO account = accountService.getAccount(id, userId);
            if (account == null) {
                return ResponseEntity.status(404).body("Account not found");
            }
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching account");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable int id, @RequestBody AccountDTO accountDTO) {
        try {
            int result = accountService.updateAccount(id, accountDTO.getUser().getId(), accountDTO.getAccountType(), accountDTO.getAvailableBalance(), accountDTO.getAccountStatus());
            if (result == 0) {
                return ResponseEntity.status(404).body("Account not found");
            }
            return ResponseEntity.ok("Account updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating account");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchAccount(@PathVariable int id, @RequestBody AccountDTO accountDTO) {
        try {
            int result = accountService.updateAccount(id, accountDTO.getUser().getId(), accountDTO.getAccountType(), accountDTO.getAvailableBalance(), accountDTO.getAccountStatus());
            if (result == 0) {
                return ResponseEntity.status(404).body("Account not found");
            }
            return ResponseEntity.ok("Account updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating account");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable int id, @RequestParam("userId") String userId) {
        try {
            int result = accountService.deleteAccount(id, userId);
            if (result == 0) {
                return ResponseEntity.status(404).body("Account not found");
            }
            return ResponseEntity.ok("Account deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting account");
        }
    }
}
