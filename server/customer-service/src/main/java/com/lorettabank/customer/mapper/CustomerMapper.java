package com.lorettabank.customer.mapper;

import com.lorettabank.customer.dto.CreateCustomerRequest;
import com.lorettabank.customer.dto.CustomerResponse;
import com.lorettabank.customer.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(CustomerEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kycStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerEntity toEntity(CreateCustomerRequest request);
}
