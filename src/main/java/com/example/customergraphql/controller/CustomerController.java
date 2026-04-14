package com.example.customergraphql.controller;

import com.example.customergraphql.model.Customer;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class CustomerController {

    private final Map<String, Customer> customers;

    public CustomerController() {
        List<Customer> customerList = List.of(
                new Customer("1", "Alice Johnson", "1990-05-15"),
                new Customer("2", "Bob Smith", "1985-11-22"),
                new Customer("3", "Charlie Brown", "2000-01-30")
        );
        this.customers = customerList.stream()
                .collect(Collectors.toMap(Customer::id, Function.identity()));
    }

    @QueryMapping
    public Customer customerById(@Argument String id) {
        return customers.get(id);
    }
}
