package com.swishsales.concurrent.entity;

import java.util.Objects;

public class Customer {

    private final String id;
    private String name;
    private String document;
    private String email;
    private Integer age;

    public Customer(String id, String name, String document, String email, Integer age) {
        this.id = id;
        this.name = name;
        this.document = document;
        this.email = email;
        this.age = age;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Customer customer)) return false;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
