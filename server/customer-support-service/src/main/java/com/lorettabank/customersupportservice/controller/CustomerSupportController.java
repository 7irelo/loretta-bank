package com.lorettabank.userservice.controller;

import com.lorettabank.userservice.dto.CustomerSupportDTO;
import com.lorettabank.userservice.service.CustomerSupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            CustomerSupportDTO createdSupport = supportService.createSupport(
                    supportDTO.getUserId(),
                    supportDTO.getQuery(),
                    supportDTO.getResponse(),
                    supportDTO.getStatus()
            );
            return ResponseEntity.status(201).body(createdSupport);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating support query");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSupport(@PathVariable int id, @RequestParam("userId") int userId) {
        try {
            CustomerSupportDTO support = supportService.getSupport(id, userId);
            if (support == null) {
                return ResponseEntity.status(404).body("Support query not found");
            }
            return ResponseEntity.ok(support);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching support query");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupport(@PathVariable int id, @RequestBody CustomerSupportDTO supportDTO) {
        try {
            int result = supportService.updateSupport(id, supportDTO.getUserId(), supportDTO.getQuery(), supportDTO.getResponse(), supportDTO.getStatus());
            if (result == 0) {
                return ResponseEntity.status(404).body("Support query not found");
            }
            return ResponseEntity.ok("Support query updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating support query");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupport(@PathVariable int id, @RequestParam("userId") int userId) {
        try {
            int result = supportService.deleteSupport(id, userId);
            if (result == 0) {
                return ResponseEntity.status(404).body("Support query not found");
            }
            return ResponseEntity.ok("Support query deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting support query");
        }
    }
}
