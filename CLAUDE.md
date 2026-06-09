# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
mvn clean install

# Run locally (security disabled — no JWT required)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run with JWT security enabled (configure application-prod.properties first)
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Run all tests
export JAVA_HOME=$(/usr/libexec/java_home -v 17) && mvn test

# Run a specific test class
mvn test -Dtest=CustomerControllerTest

# Run a specific test method
mvn test -Dtest=CustomerControllerTest#customerById_existingId_returnsCustomer

# Run Cucumber integration tests only
mvn test -Dtest=CucumberIT

# Full build + verify
mvn verify -Dspring.profiles.active=local
```

The application starts on `http://localhost:8080`. GraphiQL is at `/graphiql`. The GraphQL endpoint is at `/graphql`.




## Architecture

**Spring Boot 4.0 / Java 17 GraphQL API** using Spring GraphQL (schema-first approach).

**Request flow:** HTTP POST `/graphql` → Spring Security filter (JWT validation in non-local profiles) → Spring GraphQL routing → `@QueryMapping` method in `CustomerController` → `Customer` record → JSON response.

**Key files:**
- `graphql/schema.graphqls` — source of truth for the API contract; drives everything. Add fields here first before implementing.
- `controller/CustomerController.java` — binds GraphQL query fields via `@QueryMapping`. Currently backed by a hardcoded in-memory `Map<String, Customer>` (no persistence layer).
- `config/SecurityConfig.java` — two `SecurityFilterChain` beans, profile-gated: `!local` enforces JWT OAuth2, `local` permits all requests. `JwtDecoder` is also profile-gated to avoid contacting the JWKS endpoint during local dev/tests.

**Spring profiles:**

| Profile | Security |
|---|---|
| `local` | All endpoints open, no token required |
| `prod` (or any non-local) | JWT required for `/graphql`; `/actuator/health`, `/actuator/info`, `/graphiql`, and static resources are public |

JWT issuer/JWKS URIs are configured in `application-prod.properties` and must be updated before deploying with the `prod` profile.

## Testing

Two distinct test layers:

1. **JUnit / `@GraphQlTest`** — `CustomerControllerTest` uses `GraphQlTester` with inline document strings (not `.documentName()`). Uses `@GraphQlTest` (slice test, not full context). Security config tests in `config/` use `MockMvc` + `@MockitoBean JwtDecoder` to prevent real JWKS network calls; most tests there are currently commented out.

2. **Cucumber integration tests** — `CucumberIT` is the JUnit Platform Suite runner; step definitions live in `steps/`; feature files live in `src/test/resources/features/`. Cucumber uses a real `@SpringBootTest` (started on a random port) and `RestTemplate` to call the actual running server. Currently covers `/actuator/health` only.

## Deploying to AWS ECS (Fargate)

Infrastructure is defined as a Java CDK app in `cdk/`. It provisions a VPC, ECS Fargate cluster, ALB, and Secrets Manager secret for JWT config.

**Prerequisites:** AWS CDK CLI, Docker (running), AWS credentials configured.

```bash
# First-time setup
npm install -g aws-cdk
cd cdk && cdk bootstrap   # once per AWS account/region

# Populate JWT config in Secrets Manager before first deploy
aws secretsmanager put-secret-value \
  --secret-id customer-graphql/jwt-config \
  --secret-string '{"jwtIssuerUri":"https://YOUR_ISSUER","jwtJwkSetUri":"https://YOUR_ISSUER/.well-known/jwks.json"}'

# Deploy (builds Docker image, pushes to ECR, deploys stack)
cd cdk && cdk deploy

# Tear down
cd cdk && cdk destroy
```

The ALB DNS name is printed as a CloudFormation output after deploy. The `/graphql` endpoint requires a Bearer JWT; `/actuator/health` and `/graphiql` are public.

**CDK stack key decisions:**
- `ContainerImage.fromRegistry(imageUri)` pulls a pre-built image from a cross-account ECR repo — no Docker build. The `imageUri` is read from CDK context (`cdk.json` or `--context imageUri=...`).
- JWT config is injected into the container via Secrets Manager (not baked into the image), mapping to Spring's `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_*` env var overrides.
- Default VPC has 2 AZs with NAT gateways (cost: ~$0.045/hr each). For dev/test, edit `CustomerGraphqlStack.java` and set `natGateways(1)` on the `VpcProps`.

## Adding Features

- **New queries/mutations:** Add the field to `schema.graphqls` first, then add a matching `@QueryMapping` or `@MutationMapping` method in `CustomerController`.
- **New data types:** Create a Java record in `model/` matching the GraphQL type fields exactly.
- **New Cucumber scenarios:** Add a `.feature` file under `src/test/resources/features/` and implement step definitions in `src/test/java/.../steps/`.
- **Database:** No persistence layer — controller uses a hardcoded in-memory map. Adding a repository layer would require wiring a `DataSource` and updating the controller constructor.