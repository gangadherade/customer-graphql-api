Feature: Actuator Health Endpoint

  Scenario: Health endpoint returns UP status
    Given the application is running
    When I call the health endpoint
    Then the response status should be 200
    And the health status should be "UP"

  Scenario: Health endpoint returns JSON content type
    Given the application is running
    When I call the health endpoint
    Then the response status should be 200
    And the response content type should contain "application/json"