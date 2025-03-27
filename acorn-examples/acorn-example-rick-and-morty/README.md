# 🧠 Acorn Java Example: Rick and Morty NLQ

This is a minimal example of using [Acorn Java](https://github.com/DataSQRL/acorn-java) to query the [Rick and Morty API](https://rickandmortyapi.com/) using **natural language**.

Acorn Java is a Java-native framework that allows you to convert natural language into executable GraphQL queries, powered by large language models (LLMs) and Acorn's schema understanding.

---

## 💡 What This Project Does

This example shows how to:

- Use **Acorn Java** to translate natural language into GraphQL queries  
- Connect to the [Rick and Morty GraphQL API](https://rickandmortyapi.com/documentation/#graphql)  
- Print the result of executing natural queries like:

  > "Show me all episodes where Morty appears"  
  > "List all characters from Earth"  
  > "What are the names of the planets in the show?"

---

## 🛠️ How It Works

1. **Schema Introspection**  
   Acorn introspects the Rick and Morty GraphQL schema and builds a natural language understanding of the available types and fields.

2. **Natural Language to Query Translation**  
   User inputs are passed to Acorn, which uses an LLM to generate a valid GraphQL query.

3. **Query Execution**  
   The query is sent to the Rick and Morty API, and the results are printed to the console.

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- OpenAI API key (used via environment variable `OPENAI_API_KEY`)

### Run the example

```bash
mvn spring-boot:run
```

Then query the API:

```
curl --location 'http://localhost:8080/agent?message=List all characters who appeared in episode Pilot'
```


## 🤖 Example Queries

> List all characters who appeared in episode "Pilot"
> Show me the episodes where Rick goes to another dimension
> What species are there in the show?
