# PhoneGraph — KG & Retrieval Service (Step 4, Option A)

This is the **first fully working microservice** from the PhoneGraph
architecture (CMP7200 dissertation proposal, Figure 1, Layer 3).

It connects to the Neo4j knowledge graph (Step 2) and the pgvector
semantic index (Step 3), and exposes REST endpoints to query both —
separately and combined ("hybrid retrieval").

**What this service does NOT do (by design):** call GPT-4o-mini. That
is the LLM Orchestrator's job (Step 5, built next). Keeping this
service retrieval-only matches the microservice boundaries in your
proposal architecture diagram.

---

## Project structure

```
phonegraph-kg-retrieval/
├── pom.xml                                  ← Maven build file
├── src/main/java/com/phonegraph/kgretrieval/
│   ├── KgRetrievalServiceApplication.java   ← Spring Boot entry point
│   ├── model/                               ← 9 KG entities + pgvector entity
│   │   ├── Phone.java          (central node)
│   │   ├── Brand.java          (MADE_BY)
│   │   ├── OS.java             (RUNS)
│   │   ├── Chipset.java        (HAS_CHIPSET)
│   │   ├── Display.java        (HAS_DISPLAY)
│   │   ├── Battery.java        (HAS_BATTERY)
│   │   ├── Camera.java         (HAS_CAMERA)
│   │   ├── Feature.java        (SUPPORTS_FEATURE)
│   │   ├── StorageVariant.java (HAS_VARIANT)
│   │   └── PhoneEmbedding.java (pgvector table mapping)
│   ├── repository/
│   │   ├── PhoneGraphRepository.java     ← Cypher queries (Neo4j)
│   │   └── PhoneEmbeddingRepository.java ← pgvector similarity query
│   ├── service/
│   │   ├── EmbeddingClient.java   ← calls Python embedding microservice
│   │   └── RetrievalService.java  ← hybrid fusion logic
│   └── controller/
│       └── RetrievalController.java ← REST API
├── src/main/resources/
│   └── application.properties       ← DB connection config
├── src/test/java/...
│   └── KgRetrievalServiceApplicationTests.java
└── embedding-service/
    ├── embedding_service.py         ← tiny Flask microservice
    └── requirements.txt
```

---

## Why the embedding microservice exists

Step 3's Python script embedded all 2,000 phones using the
`all-MiniLM-L6-v2` model. To search by meaning, a user's typed query
must be converted into **the same 384-number format** using **the
same model** — otherwise the numbers aren't comparable.

Since that model is a Python library, this project runs a tiny Flask
service (`embedding-service/embedding_service.py`) that the Java
backend calls over HTTP whenever it needs to embed a query. This is a
common, realistic microservice pattern — one small service, one job.

---

## Prerequisites (should already be done from Steps 2 & 3)

- Neo4j running via Docker (`phonegraph-neo4j/docker-compose.yml`), with 2,000 phones loaded
- PostgreSQL + pgvector running via Docker (`phonegraph-pgvector/docker-compose.yml`), with 2,000 embeddings loaded
- Java 21 installed
- Maven installed (or use the included `mvnw` wrapper if present)

---

## Setup — step by step

### 1. Start the embedding microservice

```bash
cd embedding-service
pip install -r requirements.txt
python embedding_service.py
```

Wait for: `Model ready. Embedding service listening on port 5001.`

Leave this terminal running.

### 2. Confirm Neo4j and pgvector are running

In two other terminals (or check `docker ps`):

```bash
cd ../phonegraph-neo4j
docker-compose up -d

cd ../phonegraph-pgvector
docker-compose up -d
```

### 3. Build and run the Spring Boot service

In a new terminal, from the `phonegraph-kg-retrieval` folder:

```bash
mvn spring-boot:run
```

Wait for: `Started KgRetrievalServiceApplication in X seconds`

The service runs on **http://localhost:8081**

---

## Testing the endpoints

Open these URLs in your browser, or use `curl` / Postman:

### Health check
```
http://localhost:8081/api/retrieval/health
```

### Brand statistics (sanity check — should show ~2000 phones total across brands)
```
http://localhost:8081/api/retrieval/stats/brands
```

### Graph-only search (exact constraints via Cypher)
```
http://localhost:8081/api/retrieval/graph?brand=Samsung&maxPrice=500&feature=5G&limit=5
```

### Semantic-only search (meaning-based via pgvector)
```
http://localhost:8081/api/retrieval/semantic?query=best camera phone under 500 dollars&limit=5
```

### Hybrid search (both combined — the core PhoneGraph approach)
```
http://localhost:8081/api/retrieval/hybrid?query=good camera phone&brand=Samsung&maxPrice=800&limit=5
```

---

## What "done" looks like

You should be able to:
1. Hit `/health` and get a plain text response
2. Hit `/stats/brands` and see real brand counts from your 2,000 phones
3. Hit `/graph` and get back structured Phone objects with exact matches
4. Hit `/semantic` and get back phones ranked by meaning similarity
5. Hit `/hybrid` and see both result sets fused together

If all 5 work, **Step 4 (Option A) is complete** — you have one fully
working microservice connecting the whole retrieval half of PhoneGraph.

---

## Next step (Step 5)

Build the **LLM Orchestrator** service, which will:
1. Call this service's `/hybrid` endpoint to get candidate phones
2. Build a constrained prompt from the results
3. Send it to GPT-4o-mini
4. Check the response against the KG whitelist (hallucination prevention)
5. Return the final answer

This matches Figure 1 (Layer 3) and Section 3.6 of your proposal.
