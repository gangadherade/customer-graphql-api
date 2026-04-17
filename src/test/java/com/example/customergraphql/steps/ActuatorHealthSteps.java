package com.example.customergraphql.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;


@ScenarioScope
public class ActuatorHealthSteps {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    private ResponseEntity<String> response;

    @Given("the application is running")
    public void theApplicationIsRunning() {
        // Spring Boot test context is already started
    }

    @When("I call the health endpoint")
    public void iCallTheHealthEndpoint() {
        String url = "http://localhost:" + port + "/actuator/health";
        response = restTemplate.getForEntity(url, String.class);
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @And("the health status should be {string}")
    public void theHealthStatusShouldBe(String expectedStatus) {
        assertThat(response.getBody()).contains("\"status\":\"" + expectedStatus + "\"");
    }

    @And("the response content type should contain {string}")
    public void theResponseContentTypeShouldContain(String expectedContentType) {
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).contains(expectedContentType);
    }
}