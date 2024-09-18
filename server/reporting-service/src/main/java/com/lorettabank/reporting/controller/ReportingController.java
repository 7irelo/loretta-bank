package com.lorettabank.reporting.controller;

import com.lorettabank.reporting.dto.GenerateStatementRequest;
import com.lorettabank.reporting.dto.StatementResponse;
import com.lorettabank.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reporting", description = "Statements and reporting endpoints")
public class ReportingController {

    private final ReportingService reportingService;

    @PostMapping("/statements")
    @Operation(summary = "Generate a statement for an account and period")
    public ResponseEntity<StatementResponse> generateStatement(
            @Valid @RequestBody GenerateStatementRequest request) {
        StatementResponse response =
                reportingService.generateStatement(request, getAuthenticatedUserId(), getRoles());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/statements/{id}")
    @Operation(summary = "Get statement by ID")
    public ResponseEntity<StatementResponse> getStatement(@PathVariable Long id) {
        StatementResponse response =
                reportingService.getStatement(id, getAuthenticatedUserId(), getRoles());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statements/account/{accountId}")
    @Operation(summary = "Get all statements for account")
    public ResponseEntity<java.util.List<StatementResponse>> getStatementsForAccount(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(
                reportingService.getStatementsForAccount(
                        accountId, getAuthenticatedUserId(), getRoles()));
    }

    @GetMapping("/statements/customer/{customerId}")
    @Operation(summary = "Get all statements for customer")
    public ResponseEntity<java.util.List<StatementResponse>> getStatementsForCustomer(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(
                reportingService.getStatementsForCustomer(
                        customerId, getAuthenticatedUserId(), getRoles()));
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(authentication.getPrincipal().toString());
    }

    private Set<String> getRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
    }
}
