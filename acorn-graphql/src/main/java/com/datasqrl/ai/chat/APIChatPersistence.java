package com.datasqrl.ai.chat;

import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.api.APIQueryExecutor;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.util.ErrorHandling;
import com.datasqrl.ai.tool.FunctionUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Slf4j
public class APIChatPersistence implements ChatPersistence {

  APIQueryExecutor apiExecutor;
  APIQuery saveMessage;
  APIQuery getMessages;
  Set<String> getMessageContextKeys;

  /**
   * Saves the generic chat message with the configured context asynchronously (i.e. does not block)
   *
   * @param message chat message to save
   * @return A future for this asynchronous operation which returns the result as a string.
   */
  public CompletableFuture<String> saveChatMessage(@NonNull Object message, @NonNull Context context) {
    ObjectMapper mapper = apiExecutor.getObjectMapper();
    ObjectNode payload = mapper.valueToTree(message);
    //Inline context variables
    context.forEach((k, v) -> {
      if (payload.has(k)) {
        log.warn("Context variable overlaps with message field and is ignored: {}", k);
      } else {
        payload.set(k, mapper.valueToTree(v));
      }
    });;
    try {
      return apiExecutor.executeQueryAsync(saveMessage, payload);
    } catch (IOException e) {
      log.error("Failed to save chat message: ", e);
      return CompletableFuture.failedFuture(e);
    }
  }

  /**
   * Retrieves saved chat messages from the API via the configured function call.
   * If no function call for message retrieval is configured, an empty list is returned.
   *
   * Uses the configured context to retrieve user or context specific chat messages.
   *
   * @param context Arbitrary session context that identifies a user or provides contextual information.
   * @return Saved messages for the provided context
   */
  public <ChatMessage> List<ChatMessage> getChatMessages(
      @NonNull Context context, int limit, @NonNull Class<ChatMessage> clazz) throws IOException {
    ObjectMapper mapper = apiExecutor.getObjectMapper();
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put("limit", limit);
    JsonNode variables = FunctionUtil.addOrOverrideContext(arguments, getMessageContextKeys, context, mapper);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    String response = apiExecutor.executeQuery(getMessages, variables);
    JsonNode root = mapper.readTree(response);
    JsonNode messages = root.path("data").path("messages");

    List<ChatMessage> chatMessages = new ArrayList<>();
    for (JsonNode node : messages) {
      ChatMessage chatMessage = mapper.treeToValue(node, clazz);
      chatMessages.add(chatMessage);
    }
    Collections.reverse(chatMessages); //newest should be last
    return chatMessages;

  }


}
