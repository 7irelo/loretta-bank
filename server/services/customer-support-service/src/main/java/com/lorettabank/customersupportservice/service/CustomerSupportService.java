package com.lorettabank.customersupportservice.service;

import com.lorettabank.customersupportservice.dto.CustomerSupportDTO;
import com.lorettabank.customersupportservice.dto.TransactionDTO;
import com.lorettabank.customersupportservice.dto.UserDTO;
import com.lorettabank.customersupportservice.repository.CustomerSupportRepository;
import com.lorettabank.customersupportservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerSupportService {

    private final CustomerSupportRepository supportRepository;
    private final TransactionRepository transactionRepository;

    public CustomerSupportService(CustomerSupportRepository supportRepository, TransactionRepository transactionRepository) {
        this.supportRepository = supportRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public CustomerSupportDTO createSupport(CustomerSupportDTO supportDTO) {
        return supportRepository.save(supportDTO);
    }

    public Optional<CustomerSupportDTO> getSupport(Long id) {
        return supportRepository.findById(id)
            .map(this::attachLatestTransactions);
    }

    @Transactional
    public CustomerSupportDTO updateSupport(Long id, CustomerSupportDTO supportDTO) {
        return supportRepository.findById(id)
            .map(existingSupport -> {
                existingSupport.setQuery(supportDTO.getQuery());
                existingSupport.setResponse(supportDTO.getResponse());
                existingSupport.setStatus(supportDTO.getStatus());
                return supportRepository.save(existingSupport);
            })
            .orElse(null);
    }

    @Transactional
    public boolean deleteSupport(Long id) {
        if (supportRepository.existsById(id)) {
            supportRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private CustomerSupportDTO attachLatestTransactions(CustomerSupportDTO supportDTO) {
        UserDTO user = supportDTO.getUser();
        if (user != null) {
            List<TransactionDTO> latestTransactions = transactionRepository.findLatest15ByUserId(user.getId());
            user.setLatestTransactions(latestTransactions);
        }
        return supportDTO;
    }
}
