# PhoneGraph — LLM Orchestrator Service (Step 5)

This is the microservice that actually generates AI phone
recommendations. It connects to:
1. The **KG & Retrieval Service** (Step 4, running on port 8081)
2. **GLM-5.2** via NVIDIA's free API

It implements the constrained generation + hallucination check
described in Section 3.6 of the PhoneGraph proposal.

---

## What this service does

```
User query
    │
    ▼
KgRetrievalClient → calls Step 4 service /api/retrieval/hybrid
    │
    ▼
PromptBuilder → builds a constrained prompt using ONLY those facts
    │
    ▼
NvidiaLlmClient → sends prompt to GLM-5.2
    │
    ▼
HallucinationChecker → checks response against KG whitelist
    │
    ▼
Final answer + hallucination flag returned to user
```

---

## Setup

### 1. Get your NVIDIA API key
- Go to https://build.nvidia.com
- Search for GLM-5.2 in the model catalog
- Click "Get API Key"
- Copy the key (starts with `nvapi-...`)

### 2. Paste your key into the config

Open `src/main/resources/application.properties` and replace:
```
phonegraph.nvidia.api-key=PASTE_YOUR_NVIDIA_API_KEY_HERE
```
with your real key.

### 3. Make sure Step 4 (KG & Retrieval Service) is running

This service depends on it — it must be running on port 8081.

### 4. Build and run

```bash
mvn spring-boot:run
```

Wait for: `Started LlmOrchestratorServiceApplication`

The service runs on **http://localhost:8082**

---

## Testing

### Health check
```
http://localhost:8082/api/orchestrator/health
```

### Get a recommendation (POST request — use Postman or curl)

```bash
curl -X POST http://localhost:8082/api/orchestrator/recommend \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"best camera phone under 500 dollars\", \"maxPrice\": 500, \"limit\": 5}"
```

Expected response shape:
```json
{
  "answer": "Based on the candidates provided, I recommend...",
  "candidatePhones": ["Samsung Galaxy S23", "Google Pixel 8", ...],
  "flaggedClaims": [],
  "hallucinationDetected": false,
  "modelUsed": "zai-org/glm-5.2"
}
```

If `hallucinationDetected` is `true`, check `flaggedClaims` to see
which phone names the model mentioned that were NOT in the candidate
list it was given — that's the mechanism RQ1 measures.

---

## What "done" looks like

1. Health check responds
2. POST to /recommend returns a real GLM-5.2-generated answer
3. candidatePhones shows real phones from your Neo4j knowledge graph
4. hallucinationDetected correctly flags any invented phone names

Once this works, **all 5 microservices' core logic exists** (Catalogue
can be added later, Evaluation Service is next).

---

## Next step (Step 6)

Build the 250-question benchmark and the Evaluation Service, which
will call this /recommend endpoint (and the 5 baselines) repeatedly
and compute hallucination rate, P@5, MRR, nDCG@10, latency, and cost.
