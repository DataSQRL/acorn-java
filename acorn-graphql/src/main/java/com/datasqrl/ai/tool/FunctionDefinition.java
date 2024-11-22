package com.datasqrl.ai.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Definition of a chat function that can be invoked by the language model.
 *
 * This is essentially a java definition of the json structure OpenAI and most LLMs use to represent
 * functions/tools.
 *
 * This should be updated whenever the model representation of a function changes.
 * It should not contain any extra functionality - those should be added to the respective wrapper classes.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FunctionDefinition {

  private String name;
  private String description;
  private Parameters parameters;


  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Parameters {

    private String type;
    private Map<String, Argument> properties = Map.of();
    private List<String> required = List.of();

  }

  @Data
  public static class Argument {

    private String type;
    private String description;
    private Argument items;
    @JsonProperty("enum")
    private Set<?> enumValues;
  }

}
