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
    String url = "http://localhost:" + port + "/agent/1";
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
