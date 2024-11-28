package com.datasqrl.ai.acorn;

import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.api.APIQueryExecutor;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.tool.ValidationResult;
import com.datasqrl.ai.tool.ValidationResult.ErrorType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Implements the {@link APIQueryExecutor} interface for GraphQL APIs using Spring RestTemplate as the client.
 */
@Slf4j
@Service
public class SpringGraphQLExecutor implements APIQueryExecutor {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate;
  private final String endpoint;
  private final Optional<String> authHeader;

  public SpringGraphQLExecutor(@NonNull String endpoint, @NonNull Optional<String> authHeader) {
    this.restTemplate = new RestTemplate();
    this.endpoint = endpoint;
    this.authHeader = authHeader;
  }

  @Override
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public ValidationResult validate(@NonNull FunctionDefinition functionDef, JsonNode arguments) {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    try {
      String schemaText = objectMapper.writeValueAsString(functionDef.getParameters());
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
      JsonSchema schema = factory.getSchema(schemaText);
      if (arguments == null || arguments.isEmpty()) {
        arguments = objectMapper.readTree("{}");
      }
      Set<ValidationMessage> schemaErrors = schema.validate(arguments);
      if (!schemaErrors.isEmpty()) {
        String schemaErrorsText = schemaErrors.stream().map(ValidationMessage::toString).collect(
            Collectors.joining("; "));
        return new ValidationResult(ErrorType.INVALID_JSON, "Provided arguments do not match schema: " + schemaErrorsText);
      } else {
        return ValidationResult.VALID;
      }
    } catch (JsonProcessingException e) {
      return new ValidationResult(ErrorType.INVALID_JSON, e.getMessage());
    }
  }

  @Override
  public ValidationResult validate(APIQuery query) {
    if (query.query()==null || query.query().isBlank()) return new ValidationResult(ErrorType.INVALID_ARGUMENT, "Query cannot be empty");
    return ValidationResult.VALID;
  }

  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
    HttpEntity<String> request = buildRequest(query.query(), arguments);
    log.debug("Executing query:  {}", request);
    ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);
    if (!response.getStatusCode().is2xxSuccessful()) {
      log.error("Query failed: {}", response);
      throw new IOException("Query failed: " + response);
    }
    log.debug("Query result: {}", response.getBody());
    return response.getBody();
  }

  @Override
  public CompletableFuture<String> executeQueryAsync(APIQuery query, JsonNode arguments) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return executeQuery(query, arguments);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private HttpEntity<String> buildRequest(String query, JsonNode arguments) throws IOException {
    JsonNode requestBody = objectMapper.createObjectNode()
        .put("query", query)
        .set("variables", arguments);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    authHeader.ifPresent(h -> headers.set("Authorization", h));

    return new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
  }
}