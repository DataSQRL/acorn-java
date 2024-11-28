package com.datasqrl.ai.spring;

import static com.datasqrl.ai.converter.GraphQLSchemaConverterConfig.ignorePrefix;

import com.datasqrl.ai.api.GraphQLQuery;
import com.datasqrl.ai.api.SpringGraphQLExecutor;
import com.datasqrl.ai.chat.APIChatPersistence;
import com.datasqrl.ai.converter.GraphQLSchemaConverter;
import com.datasqrl.ai.converter.GraphQLSchemaConverterConfig;
import com.datasqrl.ai.converter.StandardAPIFunctionFactory;
import com.datasqrl.ai.tool.APIFunction;
import java.util.Optional;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class Config {

  public static final String USERID_KEY = "chat_memory_conversation_userid";

  private final ResourceService resourceService;
  private final ServerProperties properties;

  public Config(ResourceService resourceService, ServerProperties properties) {
    this.resourceService = resourceService;
    this.properties = properties;
  }


  @Bean
  ChatClient chatClient(ChatClient.Builder builder) {
    GraphQLTools toolConverter = new GraphQLTools(new GraphQLSchemaConverter(
        resourceService.loadResourceFileAsString("tools/schema.graphqls"),
        GraphQLSchemaConverterConfig.builder().operationFilter(ignorePrefix("Internal")).build(),
        new StandardAPIFunctionFactory(getAPIExecutor(), Set.of("customerid"))
    ));
    return builder
        .defaultAdvisors(new AcornChatMemoryAdvisor(getMemory(), 10))
        .defaultFunctions(toolConverter.getSchemaTools())
        .build();
  }

  private SpringGraphQLExecutor getAPIExecutor() {
    return new SpringGraphQLExecutor(properties.getMemoryUrl(), Optional.empty());
  }

  private AcornChatMemory getMemory() {
    APIChatPersistence chatPersistence = new APIChatPersistence(getAPIExecutor(),
        new GraphQLQuery(resourceService.loadResourceFileAsString("memory/saveMessage.graphql")),
        new GraphQLQuery(resourceService.loadResourceFileAsString("memory/getMessage.graphql")),
        Set.of(USERID_KEY));
    return new AcornChatMemory(chatPersistence, AcornChatMemory.DEFAULT_CONTEXT_PREFIX);
  }

}