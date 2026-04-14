# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
mvn clean install

# Run the application
mvn spring-boot:run

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=CustomerControllerTest

# Run a specific test method
mvn test -Dtest=CustomerControllerTest#testCustomerById
```

The application starts on `http://localhost:8080`. GraphiQL (browser-based query editor) is available at `http://loca
lhost:8080/graphiql`. The GraphQL endpoint is at `http://localhost:8080/graphql`.

## Architecture

**Spring Boot 4.0 / Java 17 GraphQL API** using Spring GraphQL (schema-first approach).

**Request flow:** HTTP POST to `/graphql` → Spring GraphQL routing → `@QueryMapping` method in `CustomerController` → `Customer` record → JSON response.

**Key layers:**
- `graphql/schema.graphqls` — defines the API contract; this drives everything. Currently only a `Query` type with `customerById(id: ID!): Customer`.
- `controller/CustomerController.java` — annotates methods with `@QueryMapping` to bind them to GraphQL query fields. Currently backed by an in-memory `Map<String, Customer>`.
- `model/Customer.java` — immutable Java record with `id`, `name`, `dob` fields.

**Testing stack:** JUnit 5 + `GraphQlTester` (from `spring-boot-starter-graphql-test`). Use `@SpringBootTest` + `@AutoConfigureGraphQlTester` for integration tests; inject `GraphQlTester` to execute `.documentName(...)` queries against the schema.

## Adding Features

- **New queries/mutations:** Add the type/field to `schema.graphqls` first, then add a matching `@QueryMapping` or `@MutationMapping` method in the controller.
- **New data types:** Create a Java record in `model/` matching the GraphQL type fields.
- **Database:** No persistence layer exists yet — the controller uses a hardcoded in-memory map as a stand-in.