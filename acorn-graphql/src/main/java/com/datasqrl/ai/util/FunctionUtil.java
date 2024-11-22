package com.datasqrl.ai.util;

import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.FunctionDefinition;
import com.datasqrl.ai.tool.FunctionValidation;
import com.datasqrl.ai.tool.FunctionValidation.ErrorType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;

public class FunctionUtil {

  public static String toJsonString(List<FunctionDefinition> tools) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.valueToTree(tools));
  }

  public static JsonNode addOrOverrideContext(JsonNode arguments, @NonNull Set<String> contextKeys,
      @NonNull Context context, @NonNull ObjectMapper mapper) {
    // Create a copy of the original JsonNode to add context
    ObjectNode copyJsonNode;
    if (arguments == null || arguments.isEmpty()) {
      copyJsonNode = mapper.createObjectNode();
    } else {
      copyJsonNode = arguments.deepCopy();
    }
    // Add context
    for (String contextKey : contextKeys) {
      Object value = context.get(contextKey);
      if (value == null) throw new IllegalArgumentException("Missing context field: " + contextKey);
      copyJsonNode.putPOJO(contextKey, value);
    }
    return copyJsonNode;
  }

  /**
   * Validates a call to the function identified by name with the provided arguments.
   * Validates that the function exists and that the provided arguments are valid.
   *
   * @param functionDef The function definition of the called function
   * @param arguments Arguments to the function
   * @return
   */
  @SneakyThrows
  public static FunctionValidation validateFunctionCall(@NonNull FunctionDefinition functionDef, JsonNode arguments) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    String schemaText = mapper.writeValueAsString(functionDef.getParameters());
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    JsonSchema schema = factory.getSchema(schemaText);
    if (arguments==null || arguments.isEmpty()) {
      arguments = mapper.readTree("{}");
    }
    Set<ValidationMessage> schemaErrors = schema.validate(arguments);
    if (!schemaErrors.isEmpty()) {
      String schemaErrorsText = schemaErrors.stream().map(ValidationMessage::toString).collect(
          Collectors.joining("; "));
      return new FunctionValidation(ErrorType.INVALID_JSON, "Invalid Schema: " + schemaErrorsText);
    } else {
      return new FunctionValidation(ErrorType.NONE, null);
    }
  }


}
