package com.datasqrl.ai.example;

import static com.datasqrl.ai.converter.GraphQLSchemaConverterConfig.ignorePrefix;

import com.datasqrl.ai.acorn.AcornSpringAIUtils;
import com.datasqrl.ai.acorn.SpringGraphQLExecutor;
import com.datasqrl.ai.api.GraphQLQuery;
import com.datasqrl.ai.chat.APIChatPersistence;
import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import com.datasqrl.ai.converter.GraphQLSchemaConverterConfig;
import com.datasqrl.ai.converter.StandardAPIFunctionFactory;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * An example configuration for loading the GraphQL schema and message storage and retrieval queries
 * to use Acorn with Chat persistence.
 *
 * <p>Uses `customerid` as the default context key.
 */
@Configuration
class Config {

  public static final String USERID_KEY = "chat_memory_conversation_userid";

  private final ResourceLoader resourceLoader;
  private final ServerProperties properties;

  public Config(ResourceLoader resourceLoader, ServerProperties properties) {
    this.resourceLoader = resourceLoader;
    this.properties = properties;
  }

  private Resource getResourceFromClasspath(String path) {
    return resourceLoader.getResource("classpath:" + path);
  }

  private String loadResourceFileAsString(String path) {
    return AcornSpringAIUtils.loadResourceAsString(getResourceFromClasspath(path));
  }

  @Bean
  GraphQLSchemaConverter graphQLSchemaConverter() {
    return new GraphQLSchemaConverter(
        loadResourceFileAsString("tools/schema.graphqls"),
        GraphQLSchemaConverterConfig.builder().operationFilter(ignorePrefix("Internal")).build(),
        new StandardAPIFunctionFactory(getAPIExecutor(), Set.of("customerid")));
  }

  private SpringGraphQLExecutor getAPIExecutor() {
    return new SpringGraphQLExecutor(properties.getBackendUrl(), Optional.empty());
  }

  @Bean
  APIChatPersistence chatPersistence() {
    return new APIChatPersistence(
        getAPIExecutor(),
        new GraphQLQuery(loadResourceFileAsString("memory/saveMessage.graphql")),
        new GraphQLQuery(loadResourceFileAsString("memory/getMessage.graphql")),
        Set.of(USERID_KEY));
  }
}
