package com.lorettabank.transaction.controller;

import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import com.lorettabank.transaction.client.AccountClient;
import com.lorettabank.transaction.client.CustomerClient.CustomerSummary;
import com.lorettabank.transaction.dto.AccountBalanceResponse;
import com.lorettabank.transaction.dto.TransactionResponse;
import com.lorettabank.transaction.dto.TransferRequest;
import com.lorettabank.transaction.dto.TransferResponse;
import com.lorettabank.transaction.entity.TransactionType;
import com.lorettabank.transaction.mapper.TransactionMapper;
import com.lorettabank.transaction.service.PaymentPolicy;
import com.lorettabank.transaction.service.TransactionService;
import com.lorettabank.transaction.service.TransactionService.Instruction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Transfers between own accounts and payments to others")
@RequiredArgsConstructor
@Validated
public class TransferController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final AccountClient accountClient;
    private final CallerContext caller;
    private final PaymentPolicy paymentPolicy;

    @PostMapping
    @Operation(summary = "Transfer to your own account or pay another account by number")
    public ResponseEntity<TransferResponse> initiateTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") @Size(min = 8, max = 128) String idempotencyKey) {
        if (!caller.hasRole(JwtConstants.ROLE_CUSTOMER)
                && !caller.hasRole(JwtConstants.ROLE_ADMIN)) {
            throw new ForbiddenException("Only customers or admins can move money");
        }

        AccountBalanceResponse source = accountClient.getAccount(request.getSourceAccountId());
        AccountBalanceResponse target = resolveTarget(request);
        if (source.getId().equals(target.getId())) {
            throw new BusinessException("Source and target accounts must be different");
        }

        if (!caller.hasRole(JwtConstants.ROLE_ADMIN)) {
            caller.assertOwns(source);
            CustomerSummary customer = caller.customer();
            paymentPolicy.assertCustomerMayPay(customer, request.getAmount());
        }

        TransactionResponse response =
                transactionService.execute(
                        new Instruction(
                                TransactionType.TRANSFER,
                                caller.userId() + ":" + idempotencyKey,
                                source.getId(),
                                target.getId(),
                                source.getCustomerId(),
                                target.getCustomerId(),
                                request.getAmount(),
                                request.getCurrency(),
                                request.getDescription(),
                                caller.userId()));
        return ResponseEntity.status(TransactionController.statusFor(response))
                .body(transactionMapper.toTransferResponse(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer status by ID")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String id) {
        TransactionResponse response = transactionService.getTransaction(id);
        if (!"TRANSFER".equals(response.getType())) {
            throw new com.lorettabank.shared.exception.ResourceNotFoundException(
                    "Transfer not found");
        }
        if (!caller.isStaff()) {
            List<Long> mine = caller.customerAccountIds();
            if (!mine.contains(response.getSourceAccountId())
                    && !mine.contains(response.getTargetAccountId())) {
                throw new ForbiddenException("You can only view your own transfers");
            }
        }
        return ResponseEntity.ok(transactionMapper.toTransferResponse(response));
    }

    private AccountBalanceResponse resolveTarget(TransferRequest request) {
        if (request.getTargetAccountId() != null) {
            return accountClient.getAccount(request.getTargetAccountId());
        }
        if (request.getTargetAccountNumber() != null) {
            return accountClient.getAccountByNumber(request.getTargetAccountNumber());
        }
        throw new BusinessException("Provide targetAccountId or targetAccountNumber");
    }
}
