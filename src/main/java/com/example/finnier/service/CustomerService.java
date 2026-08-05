package com.example.finnier.service;

import com.example.finnier.dto.CustomerRequest;
import com.example.finnier.dto.CustomerResponse;
import com.example.finnier.entity.Customer;
import com.example.finnier.entity.User;
import com.example.finnier.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final UserService userService;

    public CustomerService(CustomerRepository customerRepository, UserService userService) {
        this.customerRepository = customerRepository;
        this.userService = userService;
    }

    public Customer findCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public CustomerResponse createCustomer(CustomerRequest customerRequest) {
        User user = userService.getUserEntityByEmail(customerRequest.email());
        Customer customer = mapToEntity(customerRequest, user);
        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponse(savedCustomer);
    }

    public List<CustomerResponse> getAllCustomers(Pageable pageable) {
        Page<Customer> customers = customerRepository.findAll(pageable);
        return customers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CustomerResponse getCustomerById(Long customerId) {
        Customer customer = findCustomerById(customerId);
        return mapToResponse(customer);
    }

    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return mapToResponse(customer);
    }

    public CustomerResponse updateCustomer(Long customerId, CustomerRequest customerRequest) {
        Customer existingCustomer = findCustomerById(customerId);
        User user = userService.getUserEntityByEmail(customerRequest.email());
        existingCustomer.setUser(user);
        existingCustomer.setPhoneNumber(customerRequest.phoneNumber());
        existingCustomer.setAddress(customerRequest.address());
        Customer updatedCustomer = customerRepository.save(existingCustomer);
        return mapToResponse(updatedCustomer);
    }

    public void deleteCustomer(Long customerId) {
        Customer customer = findCustomerById(customerId);
        customerRepository.delete(customer);
    }
    private Customer mapToEntity(CustomerRequest customerRequest, User user) {
        return Customer.builder()
                .user(user)
                .phoneNumber(customerRequest.phoneNumber())
                .address(customerRequest.address())
                .build();
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getUser().getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getLoyaltyPoints(),
                customer.getDateJoined(),
                customer.getLastUpdated()
        );
    }
}
