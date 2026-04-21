package com.example.customergraphql.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void customerById_existingId_returnsCustomer() {
        graphQlTester.document("{ customerById(id: \"1\") { id name dob } }")
                .execute()
                .path("customerById.id").entity(String.class).isEqualTo("1")
                .path("customerById.name").entity(String.class).isEqualTo("Alice Johnson")
                .path("customerById.dob").entity(String.class).isEqualTo("1990-05-15");
    }

    @Test
    void customerById_secondCustomer_returnsCorrectData() {
        graphQlTester.document("{ customerById(id: \"2\") { id name dob } }")
                .execute()
                .path("customerById.id").entity(String.class).isEqualTo("2")
                .path("customerById.name").entity(String.class).isEqualTo("Bob Smith")
                .path("customerById.dob").entity(String.class).isEqualTo("1985-11-22");
    }

    @Test
    void customerById_thirdCustomer_returnsCorrectData() {
        graphQlTester.document("{ customerById(id: \"3\") { id name dob } }")
                .execute()
                .path("customerById.id").entity(String.class).isEqualTo("3")
                .path("customerById.name").entity(String.class).isEqualTo("Charlie Brown")
                .path("customerById.dob").entity(String.class).isEqualTo("2000-01-30");
    }

    @Test
    void customerById_unknownId_returnsNull() {
        graphQlTester.document("{ customerById(id: \"999\") { id name dob } }")
                .execute()
                .path("customerById").valueIsNull();
    }

    @Test
    void customerById_onlyRequestedFieldsAreReturned() {
        graphQlTester.document("{ customerById(id: \"1\") { name } }")
                .execute()
                .path("customerById.name").entity(String.class).isEqualTo("Alice Johnson")
                .path("customerById.id").pathDoesNotExist();
    }
}