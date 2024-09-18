package com.lorettabank.account.mapper;

import com.lorettabank.account.dto.AccountResponse;
import com.lorettabank.account.dto.CreateAccountRequest;
import com.lorettabank.account.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(AccountEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "overdraftEnabled", ignore = true)
    @Mapping(target = "overdraftLimit", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AccountEntity toEntity(CreateAccountRequest request);
}
