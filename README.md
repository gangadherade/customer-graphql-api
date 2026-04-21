# Customer GraphQL API

A Spring Boot 4.0 GraphQL API for querying customer data, with OAuth2 JWT security and Cucumber integration tests.

## Tech Stack

- Java 17
- Spring Boot 4.0
- Spring GraphQL
- Spring Security + OAuth2 Resource Server (JWT)
- Maven
- Cucumber 7 (integration tests)

## Getting Started

### Prerequisites

- Java 17
- Maven

### Run locally (security disabled)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Run with security enabled

Set your auth server values in `src/main/resources/application-prod.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://your-auth-server.com/issuer
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://your-auth-server.com/.well-known/jwks.json
```

Then run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## GraphQL Endpoint

| Endpoint | Description |
|---|---|
| `POST /graphql` | GraphQL API endpoint |
| `GET /graphiql` | Browser-based GraphQL query editor |

### Example Query

```graphql
query {
  customerById(id: "1") {
    id
    name
    dob
  }
}
```

### Example Response

```json
{
  "data": {
    "customerById": {
      "id": "1",
      "name": "Alice Johnson",
      "dob": "1990-05-15"
    }
  }
}
```

### Via curl

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ customerById(id: \"1\") { id name dob } }"}'
```

## Security

Security behaviour is controlled by Spring profiles:

| Profile | Behaviour |
|---|---|
| `local` | All endpoints open, no token required |
| `prod` (or any non-local) | JWT required for `/graphql`; `/actuator/health`, `/actuator/info`, `/graphiql`, and static resources are public |

Always pass a Bearer token when calling protected endpoints in non-local environments:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"query": "{ customerById(id: \"1\") { id name dob } }"}'
```

## Running Tests

```bash
# All tests (Java 17 required)
export JAVA_HOME=$(/usr/libexec/java_home -v 17) && mvn test

# Unit + security tests only
mvn test -Dspring.profiles.active=local

# Cucumber integration tests
mvn test -Dtest=CucumberIT

# Full build + verify
mvn verify -Dspring.profiles.active=local
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/customergraphql/
│   │   ├── config/SecurityConfig.java       # OAuth2 JWT security configuration
│   │   ├── controller/CustomerController.java  # GraphQL query resolver
│   │   └── model/Customer.java              # Customer record
│   └── resources/
│       ├── application.properties           # Base config (GraphiQL enabled)
│       ├── application-local.properties     # Local profile (security off)
│       ├── application-prod.properties      # Prod profile (JWT auth server URLs)
│       └── graphql/schema.graphqls          # GraphQL schema
└── test/
    ├── java/com/example/customergraphql/
    │   ├── config/                          # Security config tests
    │   ├── controller/CustomerControllerTest.java
    │   ├── model/CustomerTest.java
    │   ├── steps/ActuatorHealthSteps.java   # Cucumber step definitions
    │   └── CucumberIT.java                  # Cucumber runner
    └── resources/features/
        └── actuator_health.feature          # Cucumber scenarios
```