package com.datasqrl.ai.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.NonNull;

public class FunctionUtil {

  public static String toJsonString(List<APIFunction> tools) throws IOException {
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


}
