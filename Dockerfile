FROM maven:3.8.5-openjdk-17 as builder
WORKDIR /app

COPY pom.xml .
COPY acorn-graphql acorn-graphql
COPY acorn-core acorn-core
COPY acorn-openai acorn-openai
COPY acorn-bedrock acorn-bedrock
COPY acorn-groq acorn-groq
COPY acorn-vertex acorn-vertex
COPY acorn-docker acorn-docker

RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /app

COPY --from=builder /app/acorn-spring/target/*.jar server.jar

COPY server-start.sh /app
RUN chmod +x /app/server-start.sh

ENTRYPOINT ["/app/server-start.sh"]
