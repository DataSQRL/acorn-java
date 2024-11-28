package com.datasqrl.ai.spring;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AIController {

  private final ChatClient chatClient;

  AIController(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @GetMapping("/agent/{customerid}")
  Map<String, String> completion(
      @PathVariable("customerid") String customerid,
      @RequestParam(value = "message", defaultValue = "What can you help me with?") String message) {
    return Map.of(
        "completion",
        chatClient.prompt()
            .advisors(advisor -> advisor.param(Config.USERID_KEY, customerid))
            .toolContext(Map.of("customerid", Integer.parseInt(customerid)))
            .user(message)
            .call()
            .content());
  }
}
