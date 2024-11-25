package com.datasqrl.ai.tool;

import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.api.APIQueryExecutor;
import com.datasqrl.ai.tool.FunctionDefinition.Parameters;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;

@Value
public class APIFunction {

  FunctionDefinition function;
  Set<String> contextKeys;
  APIQuery apiQuery;
  APIQueryExecutor apiExecutor;

  public APIFunction(@NonNull FunctionDefinition function, @NonNull Set<String> contextKeys,
      @NonNull APIQuery apiQuery, @NonNull APIQueryExecutor apiExecutor) {
    this.function = function;
    this.contextKeys = contextKeys;
    this.apiQuery = apiQuery;
    this.apiExecutor = apiExecutor;
    ValidationResult result = apiExecutor.validate(apiQuery);
    if (!result.isValid()) {
      throw new IllegalArgumentException(
          "Function [" + function.getName() + "] invalid for API [" + apiExecutor
              + "]: " + result.errorMessage());
    }
  }

  public FunctionDefinition getModelFunction() {
    Predicate<String> fieldFilter = getFieldFilter(contextKeys);
    Parameters newParams = Parameters.builder()
        .type(function.getParameters().getType())
        .required(function.getParameters().getRequired().stream()
            .filter(fieldFilter).toList())
        .properties(function.getParameters().getProperties().entrySet().stream()
            .filter(e -> fieldFilter.test(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .build();

    return FunctionDefinition.builder()
        .name(function.getName())
        .description(function.getDescription())
        .parameters(newParams)
        .build();
  }

  private static Predicate<String> getFieldFilter(Set<String> fieldList) {
    Set<String> contextFilter = fieldList.stream().map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
    return field -> !contextFilter.contains(field.toLowerCase());
  }

  public ValidationResult validate(JsonNode arguments) {
    return apiExecutor.validate(getModelFunction(), arguments);
  }

  /**
   * Executes the given function with the provided arguments and context.
   *
   * @param arguments Arguments to the function
   * @param context session context that is added to the arguments
   * @return The result as string
   * @throws IOException
   */
  public String execute(JsonNode arguments, @NonNull Context context) throws IOException {
    JsonNode variables = FunctionUtil.addOrOverrideContext(arguments, contextKeys, context, apiExecutor.getObjectMapper());
    return apiExecutor.executeQuery(apiQuery, variables);
  }

}