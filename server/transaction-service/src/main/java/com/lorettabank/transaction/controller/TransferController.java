package com.lorettabank.transaction.controller;

import com.lorettabank.transaction.dto.TransferRequest;
import com.lorettabank.transaction.dto.TransferResponse;
import com.lorettabank.transaction.client.AccountClient;
import com.lorettabank.transaction.client.CustomerClient;
import com.lorettabank.transaction.service.TransferService;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Account-to-account transfer operations")
public class TransferController {

    private final TransferService transferService;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;

    public TransferController(
            TransferService transferService,
            AccountClient accountClient,
            CustomerClient customerClient) {
        this.transferService = transferService;
        this.accountClient = accountClient;
        this.customerClient = customerClient;
    }

    @PostMapping
    @Operation(summary = "Initiate a transfer between accounts")
    public ResponseEntity<TransferResponse> initiateTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        assertWriteAllowed();
        assertCustomerOwnsSourceAccount(request.getSourceAccountId());
        TransferResponse response = transferService.initiateTransfer(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer status by ID")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String id) {
        return ResponseEntity.ok(transferService.getTransfer(id));
    }

    private void assertCustomerOwnsSourceAccount(Long sourceAccountId) {
        if (!hasRole(JwtConstants.ROLE_CUSTOMER)) {
            return;
        }

        Long customerId = accountClient.getAccount(sourceAccountId).getCustomerId();
        if (customerId == null || !customerId.equals(getAuthenticatedCustomerId())) {
            throw new ForbiddenException("Customers can only transfer from their own accounts");
        }
    }

    private void assertWriteAllowed() {
        if (!hasRole(JwtConstants.ROLE_ADMIN) && !hasRole(JwtConstants.ROLE_CUSTOMER)) {
            throw new ForbiddenException("Only ADMIN or CUSTOMER can initiate transfers");
        }
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }

    private Long getAuthenticatedCustomerId() {
        return customerClient.resolveCurrentCustomerId(getAuthenticatedUserId(), getRolesHeader());
    }

    private String getRolesHeader() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }
}
