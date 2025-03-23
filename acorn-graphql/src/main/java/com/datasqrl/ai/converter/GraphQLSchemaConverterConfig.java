package com.datasqrl.ai.converter;

import graphql.language.OperationDefinition.Operation;
import java.util.Arrays;
import java.util.function.BiPredicate;
import lombok.Builder;
import lombok.Value;

/**
 * Configuration class for {@link GraphQLSchemaConverter}.
 */
@Value
@Builder
public class GraphQLSchemaConverterConfig {

  public static final GraphQLSchemaConverterConfig DEFAULT =
      GraphQLSchemaConverterConfig.builder().build();

  /**
   * Filter for selecting which operations to convert
   */
  @Builder.Default BiPredicate<Operation, String> operationFilter = (op, name) -> true;

  /**
   * The maximum depth of conversion for operations that have nested types
   */
  @Builder.Default int maxDepth = 3;

  /**
   * Returns an operations filter that filters out all operations which start with the
   * given list of prefixes.
   * @param prefixes
   * @return
   */
  public static BiPredicate<Operation, String> ignorePrefix(String... prefixes) {
    final String[] prefixesLower =
        Arrays.stream(prefixes).map(String::trim).map(String::toLowerCase).toArray(String[]::new);
    return (op, name) ->
        Arrays.stream(prefixesLower)
            .anyMatch(prefixLower -> !name.trim().toLowerCase().startsWith(prefixLower));
  }
}
