package com.datasqrl.ai.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RickAndMortyExampleApplicationTest {

  @LocalServerPort private int port;

  @Test
  void givenId_whenQueryingLLM_thenGetAnswer() throws Exception {
    String url =
        "http://localhost:"
            + port
            + "/agent?prompt=List all characters who appeared in episode Pilot";
    RestTemplate restTemplate = new RestTemplate();

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Assert that the status code is 200 OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Assert that the response body is not null and contains expected JSON content
    String responseBody = response.getBody();

    assertThat(responseBody).isNotNull().contains("Rick Sanchez");

    var json = new ObjectMapper().readTree(responseBody);
    assertThat(json.has("completion")).as("JSON must contain a 'completion' field").isTrue();
  }
}
