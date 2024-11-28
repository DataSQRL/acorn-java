package com.datasqrl.ai.spring;

import static com.datasqrl.ai.converter.GraphQLSchemaConverterConfig.ignorePrefix;

import com.datasqrl.ai.api.APIQueryExecutor;
import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import com.datasqrl.ai.converter.GraphQLSchemaConverterConfig;
import com.datasqrl.ai.converter.StandardAPIFunctionFactory;
import com.datasqrl.ai.tool.APIFunction;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;

@Value
public class GraphQLTools {

  GraphQLSchemaConverter graphQLConverter;

  public GraphQLTools(GraphQLSchemaConverter graphQLConverter) {
    this.graphQLConverter = graphQLConverter;
  }

  public FunctionCallback[] getSchemaTools() {
    return from(graphQLConverter.convertSchema());
  }

  public FunctionCallback[] getOperationTools(String operationDefinitions) {
    return from(graphQLConverter.convertOperations(operationDefinitions));
  }

  public static FunctionCallback[] from(Collection<APIFunction>... functions) {
    return Arrays.stream(functions).flatMap(Collection::stream).
        map(GraphQLTools::from).toArray(FunctionCallback[]::new);
  }

  public static FunctionCallback[] from(APIFunction... functions) {
    return Arrays.stream(functions).map(GraphQLTools::from).toArray(FunctionCallback[]::new);
  }

  public static FunctionCallback from(APIFunction function) {
    FunctionDefinition funcDef = function.getModelFunction();
    String inputSchema = toJsonString(funcDef.getParameters(), function.getApiExecutor().getObjectMapper());
    return new FunctionCallback() {
      @Override
      public String getName() {
        return funcDef.getName();
      }

      @Override
      public String getDescription() {
        return funcDef.getDescription();
      }

      @Override
      public String getInputTypeSchema() {
        return inputSchema;
      }

      @Override
      public String call(String functionInput) {
        return call(functionInput, null);
      }

      @Override
      public String call(String functionInput, ToolContext toolContext) {
        Context ctx = toolContext==null?Context.EMPTY:new ToolContextWrapper(toolContext);
        try {
          return function.validateAndExecute(functionInput, ctx);
        } catch (IOException e) { //This must be an operational exception, hence escalate
          throw new RuntimeException(e);
        }
      }
    };
  }

  private record ToolContextWrapper(ToolContext toolContext) implements Context {

    private final static Set<String> FILTERED_FIELDS = Set.of(ToolContext.TOOL_CALL_HISTORY);

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
      return objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(object);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }



}
