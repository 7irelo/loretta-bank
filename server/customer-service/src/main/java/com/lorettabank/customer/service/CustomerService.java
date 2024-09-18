package com.lorettabank.customer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.customer.dto.CreateCustomerRequest;
import com.lorettabank.customer.dto.CustomerResponse;
import com.lorettabank.customer.dto.UpdateCustomerRequest;
import com.lorettabank.customer.entity.CustomerEntity;
import com.lorettabank.customer.entity.KycStatus;
import com.lorettabank.customer.entity.OutboxEvent;
import com.lorettabank.customer.mapper.CustomerMapper;
import com.lorettabank.customer.repository.CustomerRepository;
import com.lorettabank.customer.repository.OutboxEventRepository;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.event.CustomerCreatedEvent;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.DuplicateResourceException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private static final String CACHE_PREFIX = "customer:";
    private static final String CACHE_USER_PREFIX = "customer:user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final CustomerRepository customerRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CustomerMapper customerMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateResourceException(
                    "Customer already exists for user ID: " + request.getUserId());
        }

        customerRepository
                .findByEmail(request.getEmail())
                .ifPresent(
                        existing -> {
                            throw new DuplicateResourceException(
                                    "Customer already exists with email: " + request.getEmail());
                        });

        CustomerEntity entity = customerMapper.toEntity(request);
        CustomerEntity saved = customerRepository.save(entity);

        log.info("Created customer {} for user {}", saved.getId(), saved.getUserId());

        CustomerCreatedEvent event =
                CustomerCreatedEvent.builder()
                        .customerId(saved.getId())
                        .email(saved.getEmail())
                        .firstName(saved.getFirstName())
                        .lastName(saved.getLastName())
                        .eventType("CUSTOMER_CREATED")
                        .aggregateId(String.valueOf(saved.getId()))
                        .build();
        event.initDefaults();
        saveOutboxEvent("Customer", String.valueOf(saved.getId()), "CUSTOMER_CREATED", event);

        CustomerResponse response = customerMapper.toResponse(saved);
        cacheCustomer(saved, response);
        return response;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        String cacheKey = CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof CustomerResponse response) {
            log.debug("Cache hit for customer {}", id);
            return response;
        }

        CustomerEntity entity =
                customerRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Customer not found with ID: " + id));

        CustomerResponse response = customerMapper.toResponse(entity);
        cacheCustomer(entity, response);
        return response;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByUserId(Long userId) {
        String cacheKey = CACHE_USER_PREFIX + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof CustomerResponse response) {
            log.debug("Cache hit for customer by userId {}", userId);
            return response;
        }

        CustomerEntity entity =
                customerRepository
                        .findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Customer not found for user ID: " + userId));

        CustomerResponse response = customerMapper.toResponse(entity);
        cacheCustomer(entity, response);
        return response;
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        CustomerEntity entity =
                customerRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Customer not found with ID: " + id));

        if (request.getFirstName() != null) {
            entity.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            entity.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            entity.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            entity.setAddress(request.getAddress());
        }

        CustomerEntity saved = customerRepository.save(entity);
        log.info("Updated customer {}", saved.getId());

        invalidateCache(saved);
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse updateKycStatus(Long id, KycStatus status) {
        CustomerEntity entity =
                customerRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Customer not found with ID: " + id));

        entity.setKycStatus(status);
        CustomerEntity saved = customerRepository.save(entity);
        log.info("Updated KYC status for customer {} to {}", saved.getId(), status);

        invalidateCache(saved);
        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> listCustomers(Pageable pageable) {
        Page<CustomerEntity> page = customerRepository.findAll(pageable);
        return PagedResponse.<CustomerResponse>builder()
                .content(page.getContent().stream().map(customerMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private void cacheCustomer(CustomerEntity entity, CustomerResponse response) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(CACHE_PREFIX + entity.getId(), response, CACHE_TTL);
            redisTemplate
                    .opsForValue()
                    .set(CACHE_USER_PREFIX + entity.getUserId(), response, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache customer {}: {}", entity.getId(), e.getMessage());
        }
    }

    private void invalidateCache(CustomerEntity entity) {
        try {
            redisTemplate.delete(CACHE_PREFIX + entity.getId());
            redisTemplate.delete(CACHE_USER_PREFIX + entity.getUserId());
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for customer {}: {}", entity.getId(), e.getMessage());
        }
    }

    private void saveOutboxEvent(
            String aggregateType, String aggregateId, String eventType, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .eventType(eventType)
                            .payload(payload)
                            .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize customer event");
        }
    }
}
