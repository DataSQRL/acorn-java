package com.datasqrl.ai.acorn;

import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import com.datasqrl.ai.tool.APIFunction;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.web.client.HttpClientErrorException.BadRequest;

/** Creates Spring AI's {@link FunctionCallback} from Acorn's {@link APIFunction} */
@Value
public class GraphQLTools {

  GraphQLSchemaConverter graphQLConverter;

  public GraphQLTools(GraphQLSchemaConverter graphQLConverter) {
    this.graphQLConverter = graphQLConverter;
  }

  public ToolCallback[] getSchemaTools() {
    return from(graphQLConverter.convertSchema());
  }

  public ToolCallback[] getOperationTools(String operationDefinitions) {
    return from(graphQLConverter.convertOperations(operationDefinitions));
  }

  public static ToolCallback[] from(List<APIFunction> functions) {
    return functions.stream().map(GraphQLTools::from).toArray(ToolCallback[]::new);
  }

  public static ToolCallback[] from(Collection<APIFunction>... functions) {
    return Arrays.stream(functions)
        .flatMap(Collection::stream)
        .map(GraphQLTools::from)
        .toArray(ToolCallback[]::new);
  }

  public static ToolCallback[] from(APIFunction... functions) {
    return Arrays.stream(functions).map(GraphQLTools::from).toArray(ToolCallback[]::new);
  }

  public static ToolCallback from(APIFunction function) {
    FunctionDefinition funcDef = function.getModelFunction();
    String inputSchema =
        toJsonString(funcDef.getParameters(), function.getApiExecutor().getObjectMapper());
    return new ToolCallback() {

      @Override
      public String call(String functionInput) {
        return call(functionInput, new ToolContext(Map.of()));
      }

      @Override
      public String call(String functionInput, ToolContext toolContext) {
        Context ctx = new ToolContextWrapper(toolContext);
        try {
          return function.validateAndExecute(functionInput, ctx);
        } catch (BadRequest e) {
          return "Invalid graphql Query, got the following error: " + e.getMessage();
        } catch (IOException e) { // This must be an operational exception, hence escalate
          throw new RuntimeException(e);
        }
      }

      @Override
      public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(funcDef.getName())
            .description(funcDef.getDescription())
            .inputSchema(inputSchema)
            .build();
      }
    };
  }

  private record ToolContextWrapper(ToolContext toolContext) implements Context {

    private static final Set<String> FILTERED_FIELDS = Set.of(ToolContext.TOOL_CALL_HISTORY);

    @Override
    public Object get(String key) {
      return toolContext.getContext().get(key);
    }

    @Override
    public Map<String, Object> asMap() {
      return toolContext.getContext().entrySet().stream()
          .filter(e -> !FILTERED_FIELDS.contains(e.getKey()))
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
  }

  private static String toJsonString(Object object, ObjectMapper objectMapper) {
    try {
      return objectMapper
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .writeValueAsString(object);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }
}
