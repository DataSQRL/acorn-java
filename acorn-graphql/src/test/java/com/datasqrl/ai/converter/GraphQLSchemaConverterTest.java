package com.datasqrl.ai.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.datasqrl.ai.TestUtil;
import com.datasqrl.ai.api.APIQueryExecutor;
import com.datasqrl.ai.api.MockAPIExecutor;
import com.datasqrl.ai.tool.APIFunction;
import com.datasqrl.ai.tool.FunctionUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class GraphQLSchemaConverterTest {

  public static APIQueryExecutor apiExecutor = MockAPIExecutor.of("none");

  @Test
  public void testNutshop() {
    List<APIFunction> functions = getFunctionsFromPath("graphql/nutshop-schema.graphqls");
    assertEquals(7, functions.size());
  }

  @Test
  public void testCreditCard() {
    List<APIFunction> functions = getFunctionsFromPath("graphql/creditcard-rewards.graphqls");
    assertEquals(8, functions.size());
  }

  @Test
  public void testLawEnforcement() {
    List<APIFunction> functions = getFunctionsFromPath("graphql/baseball_card.graphqls");
    assertEquals(7, functions.size());
  }

  @Test
  public void testSensors() {
    GraphQLSchemaConverter converter = getConverter(TestUtil.getResourcesFileAsString("graphql/sensors.graphqls"));
    List<APIFunction> functions = converter.convertSchema();
    assertEquals(5, functions.size());
    System.out.println(convertToJsonDefault(functions));
    APIFunction query = converter.convertOperation(TestUtil.getResourcesFileAsString("graphql/sensors-aboveTemp.graphql"));
    assertEquals("HighTemps", query.getFunction().getName());
  }

  public List<APIFunction> getFunctionsFromPath(String path) {
    return getFunctions(TestUtil.getResourcesFileAsString(path));
  }

  public List<APIFunction> getFunctions(String schemaString) {
    return getConverter(schemaString).convertSchema();
  }

  public GraphQLSchemaConverter getConverter(String schemaString) {
    StandardAPIFunctionFactory fctFactory = new StandardAPIFunctionFactory(apiExecutor, Set.of());
    return new GraphQLSchemaConverter(schemaString, new PropertiesConfiguration(), fctFactory);
  }

  @Test
  @Disabled
  public void testSchemaConversion() throws IOException {
    String schemaString = Files.readString(Path.of("../../../datasqrl-examples/finance-credit-card-chatbot/creditcard-analytics.graphqls"));
    String result = convertToJsonDefault(getFunctions(schemaString));
    System.out.println(result);
  }

  @SneakyThrows
  public static String convertToJsonDefault(List<APIFunction> functions) {
    return FunctionUtil.toJsonString(functions.stream().map(APIFunction::getFunction).toList());
  }


}
