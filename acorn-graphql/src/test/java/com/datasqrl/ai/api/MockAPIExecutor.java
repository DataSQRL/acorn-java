package com.datasqrl.ai.api;

import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.tool.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.NonNull;
import lombok.Value;

@Value
public class MockAPIExecutor implements APIQueryExecutor {

  ObjectMapper mapper = new ObjectMapper();
  Function<String, String> queryToResult;

  public static MockAPIExecutor of(@NonNull String uniformResult) {
    return new MockAPIExecutor(s -> uniformResult);
  }

  @Override
  public ObjectMapper getObjectMapper() {
    return mapper;
  }

  @Override
  public ValidationResult validate(@NonNull FunctionDefinition functionDef, JsonNode arguments) {
    return ValidationResult.VALID;
  }

  @Override
  public ValidationResult validate(APIQuery query) {
    return ValidationResult.VALID;
  }

  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
    return queryToResult.apply(query.query());
  }

  @Override
  public CompletableFuture<String> executeQueryAsync(APIQuery query, JsonNode arguments) {
    return CompletableFuture.completedFuture("mock write");
  }
}
