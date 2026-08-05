package com.example.finnier.service;

import com.example.finnier.dto.CustomerRequest;
import com.example.finnier.dto.CustomerResponse;
import com.example.finnier.entity.Customer;
import com.example.finnier.entity.User;
import com.example.finnier.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserService userService;
    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private User user;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp(){
        user = new User();
        user.setUserId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("johndoe@gmail.com");
        user.setRole(User.RoleType.USER);

        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setUser(user);
        customer.setPhoneNumber("+94234567890");
        customer.setDateJoined(LocalDateTime.now());
        customer.setLastUpdated(LocalDateTime.now());


        customerRequest = new CustomerRequest(
                user.getEmail(),
                "+94234567890",
                "123 Main St"
        );

    }

    @Test
    void createCustomer_shouldCreateNewCustomer_whenUserExists(){
        when(userService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.createCustomer(customerRequest);

        assert response != null;
        assert response.customerId().equals(customer.getCustomerId());
        assert response.email().equals(user.getEmail());

        verify (userService).getUserEntityByEmail(user.getEmail());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_shouldThrowException_whenUserDoesNotExist(){
        when(userService.getUserEntityByEmail(user.getEmail())).thenThrow(new RuntimeException("User not found"));

        try {
            customerService.createCustomer(customerRequest);
            assert false;
        } catch (RuntimeException e) {
            assert e.getMessage().equals("User not found");
        }

        verify(userService).getUserEntityByEmail(user.getEmail());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void getCustomerByEmail_shouldReturnCustomerResponse_whenCustomerExists(){
        when(customerRepository.findByUserEmail(user.getEmail())).thenReturn(java.util.Optional.of(customer));

        CustomerResponse response = customerService.getCustomerByEmail(user.getEmail());

        assert response != null;
        assert response.customerId().equals(customer.getCustomerId());
        assert response.email().equals(user.getEmail());

        verify(customerRepository).findByUserEmail(user.getEmail());
    }

    @Test
    void getCustomerByEmail_shouldThrowException_whenCustomerDoesNotExist(){
        when(customerRepository.findByUserEmail(user.getEmail())).thenReturn(Optional.empty());

        try {
            customerService.getCustomerByEmail(user.getEmail());
            assert false;
        } catch (RuntimeException e) {
            assert e.getMessage().equals("Customer not found");
        }

        verify(customerRepository).findByUserEmail(user.getEmail());
    }

    @Test
    void updateCustomer_shouldUpdateCustomer_whenCustomerExists() {
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
        when(userService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.updateCustomer(customer.getCustomerId(), customerRequest);

        assert response != null;
        assert response.customerId().equals(customer.getCustomerId());
        assert response.email().equals(user.getEmail());

        verify(customerRepository).findById(customer.getCustomerId());
        verify(userService).getUserEntityByEmail(user.getEmail());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void updateCustomer_shouldThrowException_whenCustomerDoesNotExist() {
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.empty());

        try {
            customerService.updateCustomer(customer.getCustomerId(), customerRequest);
            assert false;
        } catch (RuntimeException e) {
            assert e.getMessage().equals("Customer not found");
        }

        verify(customerRepository).findById(customer.getCustomerId());
        verify(userService, never()).getUserEntityByEmail(anyString());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void deleteCustomer_shouldDeleteCustomer_whenCustomerExists() {
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(customer.getCustomerId());

        verify(customerRepository).findById(customer.getCustomerId());
        verify(customerRepository).delete(customer);
    }

    @Test
    void deleteCustomer_shouldThrowException_whenCustomerDoesNotExist() {
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.empty());

        try {
            customerService.deleteCustomer(customer.getCustomerId());
            assert false;
        } catch (RuntimeException e) {
            assert e.getMessage().equals("Customer not found");
        }

        verify(customerRepository).findById(customer.getCustomerId());
        verify(customerRepository, never()).delete(any(Customer.class));
    }

}
