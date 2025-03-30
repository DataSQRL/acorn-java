# Acorn with Spring AI 

A Spring AI module that integrates Acorn with Spring AI for tooling and chat memory.
It provides:
- Converts GraphQL API into callback functions for LLM tooling
- Implements ChatMemory against GraphQL API

## Example Spring AI Application using Acorn

### Ricky and Morty Chatbot

TODO

### Credit Card Agent

The implementation expects that the DataSQRL rewards credit card example is running for information retrieval and message persistence.

```bash
 docker run -it -p 8888:8888 -p 8081:8081 -p 9092:9092 --rm -v $PWD:/build datasqrl/cmd:dev run -c package-rewards-local.json
```

You can then run the Spring boot application with the following settings:

* You need to set the environment variable `SPRING_AI_OPENAI_API_KEY` to your OPENAI API key
* You need to set `backedUrl` config option Spring to connect to the running DataSQRL project. On the command line you can do `--config.backendUrl=http://localhost:8888/graphql`.

Open your browser and ask messages directly via the REST endpoint:

```
http://localhost:8080/agent/4?message=what-did-you-last-tell-me
```

* The number `4` is for the customerid (1-9)
* The message is the customer message

## TODOs

* Need to update the AcornChatMemory to save and retrieve toolCalls. Those need to be mapped onto InputTypes in the GraphQL API to support their structure (see record `ToolCall` in `AssistantMessage` - it has 4 string fields). So far, I have not been able to observe a message with tool calls in the advisor, even though a tool was called. This needs to be troubleshot first.
* Need to move out the wrapper classes around acorn-graphql into a separate acorn-springai module so this can be reused more widely. Those are currently in the acorn package.
  * Need to add a helper configuration builder class to create the tools and AcornChatMemory. Allow instantiating from string and Resource. Check the current Config implementation for details. We want this to be simpler.
    * First, create APIExecutor via builder on SpringGraphQLExecutor (or multiple as needed)
    * Second, create AcornChatMemory via builder that hides APIChatPersistence
    * Third, create tools for each GraphQL schema. Create builder for easier use.
