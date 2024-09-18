package com.lorettabank.transaction.mapper;

import com.lorettabank.transaction.dto.LedgerEntryResponse;
import com.lorettabank.transaction.dto.TransactionResponse;
import com.lorettabank.transaction.dto.TransferResponse;
import com.lorettabank.transaction.entity.LedgerEntry;
import com.lorettabank.transaction.entity.Transaction;
import com.lorettabank.transaction.entity.TransferSaga;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "type", expression = "java(transaction.getType().name())")
    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    @Mapping(target = "ledgerEntries", source = "entries")
    TransactionResponse toTransactionResponse(Transaction transaction, List<LedgerEntry> entries);

    @Mapping(target = "type", expression = "java(transaction.getType().name())")
    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    @Mapping(target = "ledgerEntries", ignore = true)
    TransactionResponse toTransactionResponse(Transaction transaction);

    @Mapping(target = "entryType", expression = "java(entry.getEntryType().name())")
    LedgerEntryResponse toLedgerEntryResponse(LedgerEntry entry);

    List<LedgerEntryResponse> toLedgerEntryResponseList(List<LedgerEntry> entries);

    @Mapping(target = "transferId", source = "id")
    @Mapping(target = "status", expression = "java(saga.getStatus().name())")
    TransferResponse toTransferResponse(TransferSaga saga);
}
