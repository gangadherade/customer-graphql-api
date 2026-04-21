package com.example.customergraphql.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    @Test
    void constructor_setsAllFields() {
        Customer customer = new Customer("1", "Alice Johnson", "1990-05-15");

        assertThat(customer.id()).isEqualTo("1");
        assertThat(customer.name()).isEqualTo("Alice Johnson");
        assertThat(customer.dob()).isEqualTo("1990-05-15");
    }

    @Test
    void equality_twoCustomersWithSameValues_areEqual() {
        Customer c1 = new Customer("1", "Alice Johnson", "1990-05-15");
        Customer c2 = new Customer("1", "Alice Johnson", "1990-05-15");

        assertThat(c1).isEqualTo(c2);
    }

    @Test
    void equality_twoCustomersWithDifferentIds_areNotEqual() {
        Customer c1 = new Customer("1", "Alice Johnson", "1990-05-15");
        Customer c2 = new Customer("2", "Alice Johnson", "1990-05-15");

        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void hashCode_twoEqualCustomers_haveSameHashCode() {
        Customer c1 = new Customer("1", "Alice Johnson", "1990-05-15");
        Customer c2 = new Customer("1", "Alice Johnson", "1990-05-15");

        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        Customer customer = new Customer("1", "Alice Johnson", "1990-05-15");
        String result = customer.toString();

        assertThat(result).contains("1", "Alice Johnson", "1990-05-15");
    }

    @Test
    void fields_canBeNull() {
        Customer customer = new Customer(null, null, null);

        assertThat(customer.id()).isNull();
        assertThat(customer.name()).isNull();
        assertThat(customer.dob()).isNull();
    }
}