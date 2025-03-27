package com.datasqrl.ai.example;

import static com.datasqrl.ai.converter.GraphQLSchemaConverterConfig.ignorePrefix;

import com.datasqrl.ai.acorn.AcornSpringAIUtils;
import com.datasqrl.ai.acorn.SpringGraphQLExecutor;
import com.datasqrl.ai.chat.ChatPersistence;
import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import com.datasqrl.ai.converter.GraphQLSchemaConverterConfig;
import com.datasqrl.ai.converter.StandardAPIFunctionFactory;
import com.datasqrl.ai.tool.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * An example configuration for loading the GraphQL schema and message storage and retrieval queries
 * to use Acorn with Chat persistence.
 *
 * <p>Uses `id` as the default context key.
 */
@Configuration
class Config {
  private final ResourceLoader resourceLoader;

  public Config(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
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
        loadResourceFileAsString("schema.graphql"),
        GraphQLSchemaConverterConfig.builder().operationFilter(ignorePrefix("Internal")).build(),
        new StandardAPIFunctionFactory(getAPIExecutor(), Set.of("id")));
  }

  private SpringGraphQLExecutor getAPIExecutor() {
    return new SpringGraphQLExecutor("https://rickandmortyapi.com/graphql", Optional.empty());
  }

  @Bean
  ChatPersistence inMemoryChat() {
    var messages = new ArrayList<Object>();
    return new ChatPersistence() {

      @Override
      public CompletableFuture<String> saveChatMessage(
          @NonNull Object message, @NonNull Context context) {
        messages.add(message);
        return CompletableFuture.completedFuture("OK");
      }

      @Override
      public <ChatMessage> List<ChatMessage> getChatMessages(
          @NonNull Context context, int limit, @NonNull Class<ChatMessage> clazz)
          throws IOException {
        return messages.stream().map(clazz::cast).toList();
      }

      @Override
      public Set<String> getGetMessageContextKeys() {
        return Set.of("id");
      }
    };
  }
}
