# Customer Success ETL RAG

A small, interview-ready Spring Boot implementation of a RAG assistant using LangChain4j.

## What it demonstrates

1. Ingests multiple support documents at startup.
2. Splits them into chunks and embeds the chunks.
3. Stores embeddings in an in-memory vector store (replace this with Pinecone, Qdrant, PGVector, etc. in production).
4. Embeds the customer's question and retrieves **at most five** relevant chunks.
5. Sends the retrieved context plus the question to an LLM, then returns the answer and retrieval sources.

## Run

Requires Java 17+ and an OpenAI API key. API usage is billed separately from a ChatGPT subscription.

```powershell
$env:OPENAI_API_KEY = "your-key"
mvn spring-boot:run
```

Then call:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/support/ask `
  -ContentType 'application/json' `
  -Body '{"question":"Why is my incremental ETL load processing old records?"}'
```

## Design note

This is a **naive RAG** implementation. For production, replace the in-memory store with a persistent vector database, add tenant/document metadata filters, authenticate the endpoint, evaluate retrieval quality, and observe latency/token use. Keep the “answer only from context” rule to reduce hallucinations.
