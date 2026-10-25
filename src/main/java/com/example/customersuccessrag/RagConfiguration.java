package com.example.customersuccessrag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
class RagConfiguration {

    @Bean
    ChatModel chatModel(RagProperties properties) {
        requireApiKey(properties);
        return OpenAiChatModel.builder()
                .apiKey(properties.openaiApiKey())
                .modelName(properties.chatModel())
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel(RagProperties properties) {
        requireApiKey(properties);
        return OpenAiEmbeddingModel.builder()
                .apiKey(properties.openaiApiKey())
                .modelName(properties.embeddingModel())
                .build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    private static void requireApiKey(RagProperties properties) {
        if (properties.openaiApiKey() == null || properties.openaiApiKey().isBlank()) {
            throw new IllegalStateException("Set OPENAI_API_KEY before starting the application");
        }
    }
}
