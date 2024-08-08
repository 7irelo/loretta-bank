package com.lorettabank.customersupportservice.service;

import com.lorettabank.customersupportservice.model.CustomerSupport;
import com.lorettabank.common.model.User;
import com.lorettabank.customersupportservice.repository.CustomerSupportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerSupportServiceTest {

    @Mock
    private CustomerSupportRepository supportRepository;

    @InjectMocks
    private CustomerSupportService supportService;

    private CustomerSupport customerSupport;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        User user = new User();
        user.setId(1L);

        customerSupport = new CustomerSupport();
        customerSupport.setId(1L);
        customerSupport.setUser(user);
        customerSupport.setQuery("Test query");
        customerSupport.setResponse("Test response");
        customerSupport.setStatus("Open");
    }

    @Test
    void createSupport_ShouldReturnCreatedSupport() {
        when(supportRepository.save(any(CustomerSupport.class))).thenReturn(customerSupport);

        CustomerSupport createdSupport = supportService.createSupport(customerSupport.getUser().getId(), customerSupport.getQuery(), customerSupport.getResponse(), customerSupport.getStatus());

        assertNotNull(createdSupport);
        assertEquals(customerSupport.getQuery(), createdSupport.getQuery());
        verify(supportRepository, times(1)).save(any(CustomerSupport.class));
    }

    @Test
    void getSupport_ShouldReturnSupportWhenFound() {
        when(supportRepository.findById(1L)).thenReturn(Optional.of(customerSupport));

        CustomerSupport foundSupport = supportService.getSupport(1, 1);

        assertNotNull(foundSupport);
        assertEquals(customerSupport.getId(), foundSupport.getId());
        verify(supportRepository, times(1)).findById(1L);
    }

    @Test
    void getSupport_ShouldReturnNullWhenNotFound() {
        when(supportRepository.findById(1L)).thenReturn(Optional.empty());

        CustomerSupport foundSupport = supportService.getSupport(1, 1);

        assertNull(foundSupport);
        verify(supportRepository, times(1)).findById(1L);
    }

    @Test
    void updateSupport_ShouldReturnUpdatedCount() {
        when(supportRepository.save(any(CustomerSupport.class))).thenReturn(customerSupport);

        int result = supportService.updateSupport(1, 1, "Updated query", "Updated response", "Resolved");

        assertEquals(1, result);
        verify(supportRepository, times(1)).save(any(CustomerSupport.class));
    }

    @Test
    void deleteSupport_ShouldReturnDeletedCount() {
        doNothing().when(supportRepository).deleteById(1L);

        int result = supportService.deleteSupport(1, 1);

        assertEquals(1, result);
        verify(supportRepository, times(1)).deleteById(1L);
    }
}
