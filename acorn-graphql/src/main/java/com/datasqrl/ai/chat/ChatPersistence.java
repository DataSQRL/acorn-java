package com.datasqrl.ai.chat;

import com.datasqrl.ai.tool.Context;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

public interface ChatPersistence {

  public CompletableFuture<String> saveChatMessage(@NonNull Object message, @NonNull Context context);

  public <ChatMessage> List<ChatMessage> getChatMessages(
      @NonNull Context context, int limit, @NonNull Class<ChatMessage> clazz) throws IOException;

}
