package com.example.customersuccessrag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;

@Service
public class RagService {

    private static final String SYSTEM_PROMPT = """
            You are a customer-success engineer supporting an ETL product.
            Answer only from the supplied reference context. If the context does not establish an answer,
            say that clearly and recommend the next diagnostic step. Do not invent product behavior.
            Cite supporting source filenames in square brackets.
            """;

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagProperties properties;

    public RagService(ChatModel chatModel, EmbeddingModel embeddingModel,
                      EmbeddingStore<TextSegment> embeddingStore, RagProperties properties) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.properties = properties;
    }

    @PostConstruct
    void ingestBundledDocuments() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:docs/*");
        for (Resource resource : resources) {
            try (var input = resource.getInputStream()) {
                String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                Document document = Document.from(text, Metadata.from("file_name", resource.getFilename()));
                List<TextSegment> segments = DocumentSplitters.recursive(300, 30).split(document);
                List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
                embeddingStore.addAll(embeddings, segments);
            }
        }
    }

    public Answer answer(String question) {
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(properties.maxResults())
                        .minScore(properties.minScore())
                        .build())
                .matches();

        if (matches.isEmpty()) {
            return new Answer("I could not find relevant support documentation. Please collect the job ID, error message, and connector name.", List.of());
        }

        String context = matches.stream()
                .map(match -> "Source: " + match.embedded().metadata().getString("file_name")
                        + "\n" + match.embedded().text())
                .reduce((left, right) -> left + "\n\n---\n\n" + right)
                .orElseThrow();
        String prompt = SYSTEM_PROMPT + "\n\nREFERENCE CONTEXT:\n" + context
                + "\n\nCUSTOMER QUESTION:\n" + question;
        String response = chatModel.chat(prompt);
        List<Source> sources = matches.stream()
                .map(match -> new Source(match.embedded().metadata().getString("file_name"), match.score()))
                .toList();
        return new Answer(response, sources);
    }

    public record Answer(String answer, List<Source> sources) { }
    public record Source(String filename, double score) { }
}
