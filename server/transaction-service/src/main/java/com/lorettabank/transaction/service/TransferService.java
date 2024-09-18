package com.lorettabank.transaction.service;

import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import com.lorettabank.transaction.dto.TransferRequest;
import com.lorettabank.transaction.dto.TransferResponse;
import com.lorettabank.transaction.entity.TransferSaga;
import com.lorettabank.transaction.mapper.TransactionMapper;
import com.lorettabank.transaction.repository.TransferSagaRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferSagaRepository transferSagaRepository;
    private final TransferSagaOrchestrator sagaOrchestrator;
    private final TransactionMapper transactionMapper;

    public TransferService(
            TransferSagaRepository transferSagaRepository,
            TransferSagaOrchestrator sagaOrchestrator,
            TransactionMapper transactionMapper) {
        this.transferSagaRepository = transferSagaRepository;
        this.sagaOrchestrator = sagaOrchestrator;
        this.transactionMapper = transactionMapper;
    }

    public TransferResponse initiateTransfer(TransferRequest request, String idempotencyKey) {
        log.info(
                "Initiating transfer: source={}, target={}, amount={}, idempotencyKey={}",
                request.getSourceAccountId(),
                request.getTargetAccountId(),
                request.getAmount(),
                idempotencyKey);

        if (request.getSourceAccountId().equals(request.getTargetAccountId())) {
            throw new BusinessException("Source and target accounts must be different");
        }

        Optional<TransferSaga> existing =
                transferSagaRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Returning existing transfer saga for idempotencyKey={}", idempotencyKey);
            return transactionMapper.toTransferResponse(existing.get());
        }

        TransferSaga saga =
                TransferSaga.builder()
                        .id(UUID.randomUUID().toString())
                        .idempotencyKey(idempotencyKey)
                        .sourceAccountId(request.getSourceAccountId())
                        .targetAccountId(request.getTargetAccountId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .description(request.getDescription())
                        .build();
        transferSagaRepository.save(saga);

        sagaOrchestrator.execute(saga);

        TransferSaga updatedSaga =
                transferSagaRepository
                        .findById(saga.getId())
                        .orElse(saga);
        return transactionMapper.toTransferResponse(updatedSaga);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(String transferId) {
        TransferSaga saga =
                transferSagaRepository
                        .findById(transferId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transfer not found: " + transferId));
        return transactionMapper.toTransferResponse(saga);
    }
}
