package com.lorettabank.userservice.service;

import com.lorettabank.userservice.dto.CustomerSupportDTO;
import com.lorettabank.userservice.repository.CustomerSupportRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerSupportService {
    private final CustomerSupportRepository supportRepository;

    public CustomerSupportService(CustomerSupportRepository supportRepository) {
        this.supportRepository = supportRepository;
    }

    public CustomerSupportDTO createSupport(int userId, String query, String response, String status) {
        return supportRepository.createSupport(userId, query, response, status);
    }

    public CustomerSupportDTO getSupport(int id, int userId) {
        return supportRepository.getSupport(id, userId);
    }

    public int updateSupport(int id, int userId, String query, String response, String status) {
        return supportRepository.updateSupport(id, userId, query, response, status);
    }

    public int deleteSupport(int id, int userId) {
        return supportRepository.deleteSupport(id, userId);
    }
}
