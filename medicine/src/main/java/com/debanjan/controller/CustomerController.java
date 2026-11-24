package com.debanjan.controller;

import com.debanjan.model.Customer;
import com.debanjan.repository.CustomerRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerRepository customers;

    public CustomerController(CustomerRepository customers) {
        this.customers = customers;
    }

    @GetMapping
    public List<Customer> list() {
        return customers.findAll();
    }
}
