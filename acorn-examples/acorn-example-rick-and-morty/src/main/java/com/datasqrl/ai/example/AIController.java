package com.datasqrl.ai.example;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** An example endpoint for message completion */
@RestController
class AIController {

  private final ChatClient chatClient;

  AIController(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @GetMapping("/agent")
  Map<String, String> prompt(
      @RequestParam(value = "prompt", defaultValue = "What can you help me with?") String prompt) {
    return Map.of("completion", chatClient.prompt().user(prompt).call().content());
  }
}
