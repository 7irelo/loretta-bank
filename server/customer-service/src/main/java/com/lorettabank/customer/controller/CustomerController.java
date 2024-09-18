package com.lorettabank.customer.controller;

import com.lorettabank.customer.dto.CreateCustomerRequest;
import com.lorettabank.customer.dto.CustomerResponse;
import com.lorettabank.customer.dto.UpdateCustomerRequest;
import com.lorettabank.customer.entity.KycStatus;
import com.lorettabank.customer.service.CustomerService;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        Long authenticatedUserId = getAuthenticatedUserId();

        if (!hasRole(JwtConstants.ROLE_ADMIN)
                && !request.getUserId().equals(authenticatedUserId)) {
            throw new ForbiddenException("You can only create a customer profile for yourself");
        }

        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        CustomerResponse response = customerService.getCustomer(id);
        Long authenticatedUserId = getAuthenticatedUserId();

        if (!hasRole(JwtConstants.ROLE_ADMIN)
                && !hasRole(JwtConstants.ROLE_SUPPORT)
                && !response.getUserId().equals(authenticatedUserId)) {
            throw new ForbiddenException("You do not have permission to view this customer");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyCustomerProfile() {
        Long authenticatedUserId = getAuthenticatedUserId();
        CustomerResponse response = customerService.getCustomerByUserId(authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerResponse existing = customerService.getCustomer(id);
        Long authenticatedUserId = getAuthenticatedUserId();

        if (!hasRole(JwtConstants.ROLE_ADMIN)
                && !existing.getUserId().equals(authenticatedUserId)) {
            throw new ForbiddenException("You do not have permission to update this customer");
        }

        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/kyc")
    public ResponseEntity<CustomerResponse> updateKycStatus(
            @PathVariable Long id, @RequestParam String status) {
        if (!hasRole(JwtConstants.ROLE_ADMIN)) {
            throw new ForbiddenException("Only admins can update KYC status");
        }

        KycStatus kycStatus = KycStatus.valueOf(status.toUpperCase());
        CustomerResponse response = customerService.updateKycStatus(id, kycStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CustomerResponse>> listCustomers(
            @PageableDefault(size = 20) Pageable pageable) {
        if (!hasRole(JwtConstants.ROLE_ADMIN) && !hasRole(JwtConstants.ROLE_SUPPORT)) {
            throw new ForbiddenException("Only admins and support can list all customers");
        }

        PagedResponse<CustomerResponse> response = customerService.listCustomers(pageable);
        return ResponseEntity.ok(response);
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
