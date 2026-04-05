# Cognitia

**Enterprise Knowledge Base & AI Copilot — SaaS Platform**

A multi-tenant platform that lets organizations ingest their documents, build a private knowledge base, and interact with it through an AI-powered conversational agent with retrieval-augmented generation (RAG), role-based access control, and usage-based billing.

Built with **Java 21**, **Spring Boot 4**, **Spring AI**, **PostgreSQL + pgvector**, **Apache Kafka**, and **Redis**.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Client Application                         │
└──────────────┬──────────────────────────────────┬───────────────────┘
               │ REST API                         │ SSE (Streaming)
               ▼                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Spring Boot Application                      │
│                                                                     │
│  ┌───────────┐  ┌───────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │   Auth &   │  │ Ingestion │  │   Chat   │  │    Analytics &   │  │
│  │   Tenant   │  │  Pipeline │  │  & Agent │  │     Payments     │  │
│  └─────┬─────┘  └─────┬─────┘  └────┬─────┘  └────────┬─────────┘  │
│        │              │              │                  │            │
│  ┌─────┴──────────────┴──────────────┴──────────────────┴─────────┐ │
│  │                    Shared Infrastructure                        │ │
│  │         Kafka  ·  Redis  ·  Cloudinary  ·  Razorpay            │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
   ┌────────────┐     ┌──────────────┐     ┌─────────────┐
   │ PostgreSQL │     │  PostgreSQL  │     │    Redis     │
   │  (Primary) │     │  (pgvector)  │     │   (Cache &   │
   │            │     │  Vector DB   │     │    Memory)   │
   └────────────┘     └──────────────┘     └─────────────┘
```

---

## Core Modules

### Multi-Tenant Identity & Access

- Tenant-level data isolation enforced at the database layer via Hibernate filters
- JWT authentication with RSA key-pair signing, refresh token rotation, and HttpOnly secure cookies
- Fine-grained RBAC with configurable roles, permissions, and method-level authorization
- Email-based OTP verification, account activation, team invitations, and password reset flows
- Hierarchical **classification levels** (Public → Internal → Confidential → Restricted) and **department-scoped access** for document retrieval

### Document Ingestion Pipeline

```
                                                ┌──────────────────────┐
  Upload ──▶ Cloudinary ──▶ Resource + Job ──▶  │       Kafka          │
                                                └──────────┬───────────┘
                                                           │
                    ┌──────────────────────────────────────┘
                    ▼
            ┌───────────────┐
            │  Strategy     │──▶ Document (Tika + Token Splitter)
            │  Factory      │──▶ Tabular  (POI / OpenCSV, 50-row chunks)
            │               │──▶ Image    (Vision LLM description)
            └───────┬───────┘
                    │
                    ▼
       ┌─────────────────────────┐
       │  Contextual Enrichment  │  LLM-generated situating context
       │  + Metadata Tagging     │  per chunk (tenant, dept, clearance)
       └────────────┬────────────┘
                    │
                    ▼
           ┌────────────────┐
           │    pgvector     │  Embeddings + HNSW cosine index
           │   Vector Store  │
           └────────────────┘
```

- Async, event-driven pipeline — file upload is decoupled from processing via Kafka
- Multi-format support: PDF, DOCX, PPTX, TXT, Markdown, HTML, EPUB, spreadsheets (XLSX/CSV/ODS), and images
- Strategy pattern routes each file type through format-specific parsing, chunking, and metadata extraction
- **Contextual chunk enrichment** — an LLM generates a situating paragraph for each chunk before embedding, improving retrieval relevance
- **Automated document context extraction** — LLM infers title, description, entities, and access metadata from content
- Scheduled retry mechanism for failed ingestion jobs with status tracking

### RAG & AI Chat Agent

```
  User Query
      │
      ▼
  ┌──────────────────────────────────────────────────────┐
  │  Chat Client + Advisor Chain                         │
  │                                                      │
  │  ┌────────────────────────────┐                      │
  │  │  Quota Enforcement         │  Pre-call budget     │
  │  │  Idempotency Guard         │  Dedup protection    │
  │  │  Summarizing Memory        │  Window + LLM recap  │
  │  │  Token Analytics           │  Usage metering      │
  │  └────────────────────────────┘                      │
  │                                                      │
  │  ┌────────────────────────────────────────────────┐  │
  │  │  Augmented Tool Callbacks                      │  │
  │  │  ┌──────────────────────────────────────────┐  │  │
  │  │  │  AgentThinking injection (inner thought, │  │  │
  │  │  │  confidence) ──▶ SSE agent-step events   │  │  │
  │  │  └──────────────────────────────────────────┘  │  │
  │  │  ├─ Knowledge Search ──▶ pgvector (ABAC)       │  │
  │  │  ├─ Web Search (Tavily)                        │  │
  │  │  ├─ Web Extract (Tavily)                       │  │
  │  │  └─ Date/Time                                  │  │
  │  └────────────────────────────────────────────────┘  │
  └──────────────────────┬───────────────────────────────┘
                         │
                         ▼
  SSE Stream: markdown tokens + agent-step events + source references
```

- **Agentic RAG** — the AI agent autonomously decides when to search the knowledge base vs. use general knowledge, via a tool-calling architecture
- **Cache-Augmented Generation (CAG)** — contextual chunk enrichment at ingestion time pre-bakes LLM-generated situating context into each chunk, so retrieval benefits from richer semantic content without needing query-time re-ranking
- Metadata-filtered vector search with dynamic filter expressions enforcing tenant isolation, department scope, and classification clearance (ABAC)
- **Conversational query rewriting** — ambiguous queries are rewritten using recent chat history before vector search
- Configurable similarity threshold and top-K retrieval for precision/recall tuning
- **Tool augmentation with agent thinking** — every tool call is wrapped with an `AgentThinking` parameter (inner reasoning + confidence level) that the LLM fills; these are extracted via Spring AI's `AugmentedToolCallbackProvider` and streamed to the client as real-time `agent-step` SSE events, giving full transparency into the agent's reasoning process
- **Timed tool execution with timeline events** — each tool invocation is wrapped in a timing decorator that emits structured SSE events for tool start (with reasoning), tool result (with duration and extracted source references), and errors, enabling a step-by-step agent reasoning timeline on the frontend
- Real-time streaming via SSE with line-buffered markdown and structured source references
- Persistent chat threads with Redis-backed sliding window memory and **LLM-driven summarization** of older turns (cached in Redis with content-hash deduplication)
- Database hydration for memory cold starts when Redis data is sparse
- Dynamic tool registry with user-selectable tool subsets per request
- Structured JSON output (answer, source references, follow-up suggestions) for non-streaming mode

### Usage Analytics & Quota Enforcement

- Event-driven token metering via Kafka — tracks input/output tokens, latency, and estimated cost per request
- Tenant-level and user-level quota enforcement with **hard**, **soft**, and **hybrid** enforcement modes
- Real-time pre-call quota checks wired as Spring AI chat advisors — blocks requests before LLM invocation when limits are exceeded
- Redis-based fast counters for low-latency quota lookups with idempotency guards against duplicate charges
- Daily and monthly usage aggregation with threshold notifications and overage tracking

### Subscription & Payments

- Razorpay integration with order lifecycle management and cryptographic webhook signature verification
- Event-driven payment processing with Spring Application Events and dead-letter handling for failed webhooks
- Automated plan fulfillment — successful payments trigger plan assignment and quota provisioning; refunds revert to previous plans
- Scheduled order expiry and payment retry with distributed locking via ShedLock
- Configurable plan catalog with token limits, document quotas, user caps, pricing tiers, and trial periods

### Notifications

- Async email delivery using Thymeleaf HTML templates for OTP, activation, invitation, payment confirmation, quota warnings, and refund notifications
- Redis-backed OTP management with expiry and rate limiting

---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Runtime** | Java 21, Spring Boot 4, Spring Security, Spring AI 2.0 |
| **AI / Embeddings** | OpenAI-compatible API (Gemini, Groq), pgvector (HNSW, cosine, 1536d) |
| **Data** | PostgreSQL, Spring Data JPA, Flyway |
| **Messaging** | Apache Kafka (ingestion pipeline, usage events) |
| **Caching & Memory** | Redis (session, chat memory, OTP, quota counters, distributed locks) |
| **Storage** | Cloudinary (document & image hosting) |
| **Payments** | Razorpay (orders, webhooks, signature verification) |
| **Document Parsing** | Apache Tika, Apache POI, OpenCSV |
| **Auth** | JWT (RSA PEM), refresh token rotation, OTP, email verification |
| **DevOps** | Docker, Docker Compose, multi-stage builds |

---

## Project Structure

```
src/main/java/com/intellidesk/cognitia/
├── analytics/         Metering, quotas, plans, usage aggregation
├── chat/              Chat threads, agent tools, memory, streaming
├── config/            Kafka, Redis, storage, Swagger configuration
├── ingestion/         File upload, preprocessing strategies, vector indexing
├── notification/      Email service, OTP management
├── payments/          Razorpay integration, order lifecycle, webhooks
├── userandauth/       Tenants, users, roles, permissions, JWT security
└── utils/             Exception handling, UUID v7, shared utilities
```
