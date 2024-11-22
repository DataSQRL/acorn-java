package com.datasqrl.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.util.ConfigurationUtil;
import com.datasqrl.ai.util.FunctionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class GraphQLSchemaConverterTest {


  @Test
  @SneakyThrows
  public void testNutshopSchemaFunctions() {
    GraphQLSchemaConverter converter = new GraphQLSchemaConverter(new PropertiesConfiguration(), APIExecutorFactory.DEFAULT_NAME);
    String schemaString = ConfigurationUtil.getResourcesFileAsString(
        "graphql/nutshop-schema.graphqls");
    List<RuntimeFunctionDefinition> functions = converter.convert(schemaString);
    assertEquals(7, functions.size());
//    functions.forEach(System.out::println);

    ToolsBackend backend = ToolsBackendFactory.of(functions, Map.of(
        APIExecutorFactory.DEFAULT_NAME, new APIQueryExecutor() {
          @Override
          public void validate(APIQuery query) throws IllegalArgumentException {
            assertFalse(query.getQuery().isBlank());
          }

          @Override
          public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
            System.out.println(arguments.toString());
            return "";
          }
        }), Set.of("customerid"));
    //Save and get messages aren't counted as functions
    assertEquals(5, backend.getFunctions().size());
    assertTrue(backend.getChatMessages(Context.of(Map.of("customerid", 5)), 5, GenericChatMessage.class).isEmpty());
  }


  @Test
  @SneakyThrows
  public void testNutshopSchemaConversion() {
    String schemaString = ConfigurationUtil.getResourcesFileAsString(
        "graphql/nutshop-schema.graphqls");
    String result = convertToJsonDefault(schemaString);
    String expectedResult = ConfigurationUtil.getResourcesFileAsString(
      "graphql/nutshop-schema.tools.json");
    assertEquals(expectedResult, result);
    List<RuntimeFunctionDefinition> functions = ToolsBackendFactory.readTools(result);
    assertEquals(7, functions.size());
  }

  @Test
  @Disabled
  public void testSchemaConversion() throws IOException {
    String schemaString = Files.readString(Path.of("../../../datasqrl-examples/finance-credit-card-chatbot/creditcard-analytics.graphqls"));
    String result = convertToJsonDefault(schemaString);
    System.out.println(result);
  }

  @SneakyThrows
  public static String convertToJsonDefault(String schemaString) {
    GraphQLSchemaConverter converter = new GraphQLSchemaConverter(new PropertiesConfiguration(), APIExecutorFactory.DEFAULT_NAME);
    return FunctionUtil.toJsonString(converter.convert(schemaString));
  }


}
