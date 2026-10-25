package com.example.customersuccessrag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        String openaiApiKey,
        String chatModel,
        String embeddingModel,
        int maxResults,
        double minScore) {
}
