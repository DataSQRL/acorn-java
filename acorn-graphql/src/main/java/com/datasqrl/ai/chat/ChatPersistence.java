package com.datasqrl.ai.chat;

import com.datasqrl.ai.tool.Context;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

/** Interface for saving and retrieving messages against a GraphQL API */
public interface ChatPersistence {

  ChatPersistence NONE =
      new ChatPersistence() {

        @Override
        public CompletableFuture<String> saveChatMessage(
            @NonNull Object message, @NonNull Context context) {
          return CompletableFuture.completedFuture("disabled");
        }

        @Override
        public <ChatMessage> List<ChatMessage> getChatMessages(
            @NonNull Context context, int limit, @NonNull Class<ChatMessage> clazz)
            throws IOException {
          return List.of();
        }

        @Override
        public Set<String> getMessageContextKeys() {
          return Set.of();
        }
      };

  /**
   * Saves the given message to the API
   *
   * @param message the generic message object that is serialized with Jackson
   * @param context the sensitive context of the message. The context can contains user, session,
   *     and other information
   * @return
   */
  public CompletableFuture<String> saveChatMessage(
      @NonNull Object message, @NonNull Context context);

  /**
   * Retrieves messages from the API for a given context.
   *
   * @param context The context to retrieve messages in. Contains user and session information.
   * @param limit The maximum number of messages to retrieve
   * @param clazz The type of message to return
   * @return
   * @param <ChatMessage>
   * @throws IOException
   */
  public <ChatMessage> List<ChatMessage> getChatMessages(
      @NonNull Context context, int limit, @NonNull Class<ChatMessage> clazz) throws IOException;

  Set<String> getMessageContextKeys();
}
