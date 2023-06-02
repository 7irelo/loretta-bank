package com.lorettabank.customersupportservice.controller;

import com.lorettabank.customersupportservice.dto.CustomerSupportDTO;
import com.lorettabank.customersupportservice.service.CustomerSupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/support")
public class CustomerSupportController {

    private final CustomerSupportService supportService;

    public CustomerSupportController(CustomerSupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping
    public ResponseEntity<?> createSupport(@RequestBody CustomerSupportDTO supportDTO) {
        try {
            CustomerSupportDTO createdSupport = supportService.createSupport(supportDTO);
            return ResponseEntity.status(201).body(createdSupport);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating support query");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSupport(@PathVariable Long id) {
        try {
            Optional<CustomerSupportDTO> support = supportService.getSupport(id);
            if (support.isEmpty()) {
                return ResponseEntity.status(404).body("Support query not found");
            }
            return ResponseEntity.ok(support.get());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching support query");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupport(@PathVariable Long id, @RequestBody CustomerSupportDTO supportDTO) {
        try {
            CustomerSupportDTO updatedSupport = supportService.updateSupport(id, supportDTO);
            if (updatedSupport == null) {
                return ResponseEntity.status(404).body("Support query not found");
            }
            return ResponseEntity.ok("Support query updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating support query");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupport(@PathVariable Long id) {
        try {
            boolean deleted = supportService.deleteSupport(id);
            if (!deleted) {
                return ResponseEntity.status(404).body("Support query not found");
            }
            return ResponseEntity.ok("Support query deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting support query");
        }
    }
}
