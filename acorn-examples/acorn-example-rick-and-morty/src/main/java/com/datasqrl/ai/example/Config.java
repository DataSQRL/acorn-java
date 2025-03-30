package com.datasqrl.ai.example;

import static com.datasqrl.ai.converter.GraphQLSchemaConverterConfig.ignorePrefix;

import com.datasqrl.ai.acorn.AcornSpringAIUtils;
import com.datasqrl.ai.acorn.SpringGraphQLExecutor;
import com.datasqrl.ai.chat.ChatPersistence;
import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import com.datasqrl.ai.converter.GraphQLSchemaConverterConfig;
import com.datasqrl.ai.converter.StandardAPIFunctionFactory;
import com.datasqrl.ai.tool.Context;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.com.google.common.collect.Iterators;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
  GraphQLSchemaConverter graphQLSchemaConverter(SpringGraphQLExecutor apiExecutor) {
    return new GraphQLSchemaConverter(
        loadResourceFileAsString("schema.graphql"),
        GraphQLSchemaConverterConfig.builder().operationFilter(ignorePrefix("Internal")).build(),
        new StandardAPIFunctionFactory(apiExecutor, Set.of()));
  }

  @Bean
  ChatPersistence inMemoryChat(ObjectMapper mapper) {
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
        ObjectNode arguments = mapper.createObjectNode();
        arguments.put("limit", limit);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        JsonNode root = mapper.valueToTree(messages);
        JsonNode messages =
            Optional.ofNullable(Iterators.getOnlyElement(root.path("data").fields(), null))
                .map(Map.Entry::getValue)
                .orElse(MissingNode.getInstance());

        List<ChatMessage> chatMessages = new ArrayList<>();
        for (JsonNode node : messages) {
          ChatMessage chatMessage = mapper.treeToValue(node, clazz);
          chatMessages.add(chatMessage);
        }
        Collections.reverse(chatMessages); // newest should be last
        return chatMessages;
      }

      @Override
      public Set<String> getMessageContextKeys() {
        return Set.of();
      }
    };
  }
}
