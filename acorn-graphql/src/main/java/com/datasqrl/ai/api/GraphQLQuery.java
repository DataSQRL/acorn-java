package com.datasqrl.ai.api;

/**
 * Default GraphQL query implementation
 * @param query query string
 */
public record GraphQLQuery(String query) implements APIQuery {}
