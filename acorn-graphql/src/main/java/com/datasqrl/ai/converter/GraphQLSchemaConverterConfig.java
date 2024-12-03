package com.datasqrl.ai.converter;

import graphql.language.OperationDefinition.Operation;
import java.util.Arrays;
import java.util.function.BiPredicate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GraphQLSchemaConverterConfig {

  public static final GraphQLSchemaConverterConfig DEFAULT =
      GraphQLSchemaConverterConfig.builder().build();

  @Builder.Default BiPredicate<Operation, String> operationFilter = (op, name) -> true;

  @Builder.Default int maxDepth = 3;

  public static BiPredicate<Operation, String> ignorePrefix(String... prefixes) {
    final String[] prefixesLower =
        Arrays.stream(prefixes).map(String::trim).map(String::toLowerCase).toArray(String[]::new);
    return (op, name) ->
        Arrays.stream(prefixesLower)
            .anyMatch(prefixLower -> !name.trim().toLowerCase().startsWith(prefixLower));
  }
}
