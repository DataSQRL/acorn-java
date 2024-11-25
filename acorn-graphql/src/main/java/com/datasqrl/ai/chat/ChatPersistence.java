package com.datasqrl.ai.chat;

import com.datasqrl.ai.tool.Context;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

public interface ChatPersistence {

  ChatPersistence NONE = new ChatPersistence() {

    @Override
    public CompletableFuture<String> saveChatMessage(@NonNull Object message,
        @NonNull Context context) {
      return CompletableFuture.completedFuture("disabled");
    }

    @Override
    public <ChatMessage> List<ChatMessage> getChatMessages(@NonNull Context context, int limit,
        @NonNull Class<ChatMessage> clazz) throws IOException {
      return List.of();
    }
  };

  public CompletableFuture<String> saveChatMessage(@NonNull Object message, @NonNull Context context);

  public <ChatMessage> List<ChatMessage> getChatMessages(
      @NonNull Context context, int limit, @NonNull Class<ChatMessage> clazz) throws IOException;

}
