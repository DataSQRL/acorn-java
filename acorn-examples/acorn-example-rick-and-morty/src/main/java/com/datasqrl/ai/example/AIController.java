package com.datasqrl.ai.example;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** An example endpoint for message completion */
@RestController
class AIController {

  private final ChatClient chatClient;

  AIController(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @GetMapping("/agent/{id}")
  Map<String, String> completion(
      @PathVariable("id") String id,
      @RequestParam(value = "message", defaultValue = "What can you help me with?")
          String message) {
    return Map.of(
        "completion",
        chatClient
            .prompt()
            .advisors(advisor -> advisor.param("id", id))
            .toolContext(Map.of("id", id))
            .user(message)
            .call()
            .content());
  }
}
