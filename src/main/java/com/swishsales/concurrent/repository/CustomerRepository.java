package com.swishsales.concurrent.repository;

import com.swishsales.concurrent.entity.Customer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CustomerRepository {

    private final Map<String, Customer> customersTable = new HashMap<>();

    public CustomerRepository() {
        customersTable.put("1", new Customer("1", "Tom", "111.111.111-11", "tom@gmail.com", 20));
        customersTable.put("2", new Customer("2", "Gean", "222.222.222-22", "gean@gmail.com", 21));
        customersTable.put("3", new Customer("3", "Estefano", "333.333.333-33", "estefano@gmail.com", 26));
        customersTable.put("4", new Customer("4", "Odorico", "444.444.444-44", "odorico@gmail.com", 35));
        customersTable.put("5", new Customer("5", "Frederico", "555.555.555-55", "fred@gmail.com", 22));
    }

    public Customer findById(String id) {
        return customersTable.get(id);
    }

    // Used by the producer thread to select a random customer when creating an order
    public Customer getRandomCustomer() {
        List<Customer> values = new ArrayList<>(customersTable.values());
        return values.get(new Random().nextInt(values.size()));
    }
}