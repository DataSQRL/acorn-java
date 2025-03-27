package com.datasqrl.ai.acorn;

import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AcornConfiguration {

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
}
