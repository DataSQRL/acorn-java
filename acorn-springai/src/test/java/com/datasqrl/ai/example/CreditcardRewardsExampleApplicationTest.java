package com.datasqrl.ai.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CreditcardRewardsExampleApplicationTest {

  // Start the Docker container with the specified image and configuration
  @SuppressWarnings("resource")
  @Container
  private static final GenericContainer<?> cloudBackendContainer =
      new GenericContainer<>(DockerImageName.parse("datasqrl/examples:finance"))
          .withEnv("KAFKA_BOOTSTRAP_SERVER", "localhost:9092")
          .withEnv("KAFKA_CONSUMER_GROUP", "cloud-backend1")
          .withEnv("TZ", "UTC")
          .withExposedPorts(8888, 8081, 9092)
          .withCommand("run -c package-rewards-local.json");

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "config.backendURL",
        () -> "http://localhost:" + cloudBackendContainer.getMappedPort(8888) + "/graphql");
  }

  @Test
  void givenCustomerId_whenQueryingLLM_thenGetAnswer() throws Exception {
    // The Spring Boot application is running on port 8080 (as we've defined DEFINED_PORT without
    // overriding server.port).
    String url = "http://localhost:8080/agent/1";
    RestTemplate restTemplate = new RestTemplate();

    // Perform HTTP GET request to the /agent/1 endpoint
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Assert that the status code is 200 OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Assert that the response body is not null and contains expected JSON content
    String responseBody = response.getBody();

    assertThat(responseBody).isNotNull();

    var json = new ObjectMapper().readTree(responseBody);
    assertThat(json.has("completion")).as("JSON must contain a 'completion' field").isTrue();
  }
}
