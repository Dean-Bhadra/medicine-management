package com.debanjan.service;

import com.debanjan.model.Customer;
import com.debanjan.model.Sale;
import com.debanjan.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer getOrCreateCustomer(String phone, String name) {
        Customer customer = customerRepository.findByPhone(phone);
        if (customer == null) {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setName(name);
            customer = customerRepository.save(customer);
        }
        return customer;
    }

    public List<Sale> getCustomerPurchaseHistory(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return customer.getPurchases();
    }
}
