package com.datasqrl.ai.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

public class AcornChatMemoryAdvisor extends AbstractChatMemoryAdvisor<AcornChatMemory> {

  public AcornChatMemoryAdvisor(AcornChatMemory chatMemory) {
    this(chatMemory, AbstractChatMemoryAdvisor.DEFAULT_CHAT_MEMORY_RESPONSE_SIZE);
  }

  public AcornChatMemoryAdvisor(AcornChatMemory chatMemory, int defaultChatMemoryRetrieveSize) {
    super(chatMemory, AbstractChatMemoryAdvisor.DEFAULT_CHAT_MEMORY_CONVERSATION_ID, defaultChatMemoryRetrieveSize, true);
  }

  @Override
  public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
    advisedRequest = this.before(advisedRequest);
    AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
    this.observeAfter(advisedResponse);
    return advisedResponse;
  }

  @Override
  public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
    Flux<AdvisedResponse> advisedResponses = this.doNextWithProtectFromBlockingBefore(advisedRequest, chain, this::before);
    return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponses, this::observeAfter);
  }

  private AdvisedRequest before(AdvisedRequest request) {
    Map<String, Object> adviseContext = request.adviseContext();
    int chatMemoryRetrieveSize = this.doGetChatMemoryRetrieveSize(request.adviseContext());
    List<Message> memoryMessages = getChatMemoryStore().get(adviseContext, chatMemoryRetrieveSize);
    List<Message> advisedMessages = new ArrayList(request.messages());
    advisedMessages.addAll(memoryMessages);
    AdvisedRequest advisedRequest = AdvisedRequest.from(request).withMessages(advisedMessages).build();
    UserMessage userMessage = new UserMessage(request.userText(), request.media());
    getChatMemoryStore().add(List.of(userMessage), adviseContext);
    return advisedRequest;
  }

  private void observeAfter(AdvisedResponse advisedResponse) {
    List<Message> assistantMessages = advisedResponse.response().getResults().stream().map(
        Generation::getOutput).map(Message.class::cast).toList();
    getChatMemoryStore().add(assistantMessages, advisedResponse.adviseContext());
  }

}

