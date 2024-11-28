package com.datasqrl.ai.spring;

import com.datasqrl.ai.chat.APIChatPersistence;
import com.datasqrl.ai.chat.ChatPersistence;
import com.datasqrl.ai.tool.ContextImpl;
import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.util.ErrorHandling;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

@Value
public class AcornChatMemory implements ChatMemory {

  public static final String DEFAULT_CONTEXT_PREFIX = "chat_memory_conversation_";

  APIChatPersistence chatPersistence;
  String contextPrefix;

  @Override
  public void add(String conversationId, List<Message> messages) {
    add(messages, toMap(conversationId));
  }

  public void add(List<Message> messages, Map<String, Object> context) {
    Context acornContext = toContext(context);
    messages.forEach(msg -> chatPersistence.saveChatMessage(msg, acornContext));
  }

  @Override
  public List<Message> get(String conversationId, int lastN) {
    return get(toMap(conversationId), lastN);
  }

  public List<Message> get(Map<String, Object> context, int lastN) {
    try {
      return chatPersistence.getChatMessages(toContext(context), lastN, Message.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void clear(String conversationId) {
    throw new UnsupportedOperationException("Not currently supported");
  }

  private Map<String, Object> toMap(String conversationId) {
    return Map.of(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId);
  }

  private Context toContext(Map<String, Object> advisorContext) {
    Map<String, Object> filteredContext = new HashMap<>();
    if (contextPrefix != null) {
      advisorContext.entrySet().stream().filter(entry -> entry.getKey().startsWith(contextPrefix))
          .forEach(entry -> filteredContext.put(entry.getKey(), entry.getValue()));
    }
    //Add required keys, make sure they exist
    for (String requiredKey : chatPersistence.getGetMessageContextKeys()) {
      Object value = advisorContext.get(requiredKey);
      ErrorHandling.checkArgument(value!=null, "Advisor context does not contain required key: %s", requiredKey);
      filteredContext.put(requiredKey, value);
    }
    return new ContextImpl(filteredContext);
  }

}
