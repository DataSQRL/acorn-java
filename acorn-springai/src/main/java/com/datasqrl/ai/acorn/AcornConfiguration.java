package com.datasqrl.ai.acorn;

import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AcornConfiguration {

  @Value("${config.backend-url}")
  private String backendUrl;

  @Bean
  ChatClient chatClient(
      ChatModel model, AcornChatMemory chatMemory, GraphQLSchemaConverter graphQLSchemaConverter) {
    GraphQLTools toolConverter = new GraphQLTools(graphQLSchemaConverter);
    // Builds a chat client using Acorn's GraphQL API as tools and chat persistence
    return ChatClient.builder(model)
        .defaultAdvisors(new AcornChatMemoryAdvisor(chatMemory, 10))
        .defaultTools(toolConverter.getSchemaTools())
        .build();
  }

  @Bean
  SpringGraphQLExecutor getAPIExecutor() {
    return new SpringGraphQLExecutor(backendUrl, Optional.empty());
  }
}
