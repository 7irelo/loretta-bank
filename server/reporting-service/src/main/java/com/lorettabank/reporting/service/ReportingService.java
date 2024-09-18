package com.lorettabank.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.reporting.client.AccountClient;
import com.lorettabank.reporting.client.CustomerClient;
import com.lorettabank.reporting.client.TransactionClient;
import com.lorettabank.reporting.client.dto.AccountView;
import com.lorettabank.reporting.client.dto.CustomerView;
import com.lorettabank.reporting.client.dto.TransactionView;
import com.lorettabank.reporting.dto.GenerateStatementRequest;
import com.lorettabank.reporting.dto.StatementLineItem;
import com.lorettabank.reporting.dto.StatementResponse;
import com.lorettabank.reporting.entity.StatementEntity;
import com.lorettabank.reporting.repository.StatementRepository;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import com.lorettabank.shared.security.JwtConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final StatementRepository statementRepository;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;
    private final TransactionClient transactionClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public StatementResponse generateStatement(
            GenerateStatementRequest request, Long userId, Set<String> roles) {
        validatePeriod(request.getPeriodFrom(), request.getPeriodTo());
        String rolesHeader = toRolesHeader(roles);

        AccountView account = accountClient.getAccount(request.getAccountId(), userId, rolesHeader);
        authorizeAccountAccess(account, userId, roles, rolesHeader);

        List<TransactionView> transactions =
                transactionClient.getAllTransactionsForAccount(
                        request.getAccountId(), userId, rolesHeader);

        List<TransactionView> inPeriod =
                transactions.stream()
                        .filter(tx -> inPeriod(tx, request.getPeriodFrom(), request.getPeriodTo()))
                        .sorted(Comparator.comparing(TransactionView::getCreatedAt))
                        .toList();

        List<StatementLineItem> lineItems =
                inPeriod.stream().map(tx -> toLineItem(tx, request.getAccountId())).toList();

        BigDecimal totalCredits =
                lineItems.stream()
                        .filter(item -> "CREDIT".equals(item.getDirection()))
                        .map(StatementLineItem::getAmount)
                        .reduce(ZERO, BigDecimal::add)
                        .setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalDebits =
                lineItems.stream()
                        .filter(item -> "DEBIT".equals(item.getDirection()))
                        .map(StatementLineItem::getAmount)
                        .reduce(ZERO, BigDecimal::add)
                        .setScale(4, RoundingMode.HALF_UP);

        BigDecimal closingBalance = safeScale(account.getBalance());
        BigDecimal openingBalance =
                closingBalance.subtract(totalCredits).add(totalDebits).setScale(4, RoundingMode.HALF_UP);

        StatementEntity entity =
                StatementEntity.builder()
                        .accountId(account.getId())
                        .customerId(account.getCustomerId())
                        .accountNumber(account.getAccountNumber())
                        .currency(account.getCurrency())
                        .periodFrom(request.getPeriodFrom())
                        .periodTo(request.getPeriodTo())
                        .openingBalance(openingBalance)
                        .closingBalance(closingBalance)
                        .totalCredits(totalCredits)
                        .totalDebits(totalDebits)
                        .transactionCount(lineItems.size())
                        .lineItemsJson(serializeLineItems(lineItems))
                        .build();

        StatementEntity saved = statementRepository.save(entity);
        log.info(
                "Generated statement {} for account {} (customer={})",
                saved.getId(),
                saved.getAccountId(),
                saved.getCustomerId());

        return toResponse(saved, lineItems);
    }

    @Transactional(readOnly = true)
    public StatementResponse getStatement(Long id, Long userId, Set<String> roles) {
        StatementEntity entity =
                statementRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Statement not found with ID: " + id));

        authorizeCustomerRecordAccess(entity.getCustomerId(), userId, roles, toRolesHeader(roles));
        return toResponse(entity, deserializeLineItems(entity.getLineItemsJson()));
    }

    @Transactional(readOnly = true)
    public List<StatementResponse> getStatementsForAccount(
            Long accountId, Long userId, Set<String> roles) {
        String rolesHeader = toRolesHeader(roles);
        AccountView account = accountClient.getAccount(accountId, userId, rolesHeader);
        authorizeAccountAccess(account, userId, roles, rolesHeader);

        return statementRepository.findByAccountIdOrderByGeneratedAtDesc(accountId).stream()
                .map(entity -> toResponse(entity, deserializeLineItems(entity.getLineItemsJson())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatementResponse> getStatementsForCustomer(
            Long customerId, Long userId, Set<String> roles) {
        String rolesHeader = toRolesHeader(roles);
        authorizeCustomerRecordAccess(customerId, userId, roles, rolesHeader);

        return statementRepository.findByCustomerIdOrderByGeneratedAtDesc(customerId).stream()
                .map(entity -> toResponse(entity, deserializeLineItems(entity.getLineItemsJson())))
                .toList();
    }

    private void authorizeAccountAccess(
            AccountView account, Long userId, Set<String> roles, String rolesHeader) {
        if (roles.contains(JwtConstants.ROLE_ADMIN) || roles.contains(JwtConstants.ROLE_SUPPORT)) {
            return;
        }

        if (roles.contains(JwtConstants.ROLE_CUSTOMER)) {
            CustomerView customer = customerClient.getCurrentCustomer(userId, rolesHeader);
            if (!account.getCustomerId().equals(customer.getId())) {
                throw new ForbiddenException("You can only access statements for your own accounts");
            }
            return;
        }

        throw new ForbiddenException("Insufficient permissions");
    }

    private void authorizeCustomerRecordAccess(
            Long customerId, Long userId, Set<String> roles, String rolesHeader) {
        if (roles.contains(JwtConstants.ROLE_ADMIN) || roles.contains(JwtConstants.ROLE_SUPPORT)) {
            return;
        }

        if (roles.contains(JwtConstants.ROLE_CUSTOMER)) {
            CustomerView customer = customerClient.getCurrentCustomer(userId, rolesHeader);
            if (!customerId.equals(customer.getId())) {
                throw new ForbiddenException("You can only access your own statements");
            }
            return;
        }

        throw new ForbiddenException("Insufficient permissions");
    }

    private void validatePeriod(LocalDateTime periodFrom, LocalDateTime periodTo) {
        if (periodTo.isBefore(periodFrom)) {
            throw new BusinessException("periodTo must be after periodFrom");
        }
    }

    private boolean inPeriod(
            TransactionView transaction, LocalDateTime periodFrom, LocalDateTime periodTo) {
        if (transaction.getCreatedAt() == null) {
            return false;
        }

        LocalDateTime occurredAt =
                LocalDateTime.ofInstant(transaction.getCreatedAt(), ZoneOffset.UTC);
        return !occurredAt.isBefore(periodFrom) && !occurredAt.isAfter(periodTo);
    }

    private StatementLineItem toLineItem(TransactionView transaction, Long accountId) {
        return StatementLineItem.builder()
                .transactionId(transaction.getId())
                .createdAt(transaction.getCreatedAt())
                .type(transaction.getType())
                .direction(resolveDirection(transaction, accountId))
                .amount(safeScale(transaction.getAmount()))
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .reference(transaction.getReference())
                .build();
    }

    private String resolveDirection(TransactionView transaction, Long accountId) {
        if (accountId.equals(transaction.getSourceAccountId())
                && !accountId.equals(transaction.getTargetAccountId())) {
            return "DEBIT";
        }
        if (accountId.equals(transaction.getTargetAccountId())) {
            return "CREDIT";
        }
        if ("WITHDRAWAL".equals(transaction.getType())) {
            return "DEBIT";
        }
        return "CREDIT";
    }

    private StatementResponse toResponse(StatementEntity entity, List<StatementLineItem> lineItems) {
        return StatementResponse.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .customerId(entity.getCustomerId())
                .accountNumber(entity.getAccountNumber())
                .currency(entity.getCurrency())
                .periodFrom(entity.getPeriodFrom())
                .periodTo(entity.getPeriodTo())
                .openingBalance(entity.getOpeningBalance())
                .closingBalance(entity.getClosingBalance())
                .totalCredits(entity.getTotalCredits())
                .totalDebits(entity.getTotalDebits())
                .transactionCount(entity.getTransactionCount())
                .generatedAt(entity.getGeneratedAt())
                .lineItems(lineItems)
                .build();
    }

    private String serializeLineItems(List<StatementLineItem> lineItems) {
        try {
            return objectMapper.writeValueAsString(lineItems);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize statement line items");
        }
    }

    private List<StatementLineItem> deserializeLineItems(String lineItemsJson) {
        try {
            return objectMapper.readValue(
                    lineItemsJson, new TypeReference<List<StatementLineItem>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to deserialize statement line items");
        }
    }

    private String toRolesHeader(Set<String> roles) {
        return roles.stream().sorted().collect(Collectors.joining(","));
    }

    private BigDecimal safeScale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
