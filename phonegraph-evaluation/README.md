# PhoneGraph — Evaluation Service (Step 7, Phase A)

Runs the 250-question benchmark (Step 6) against PhoneGraph's full
pipeline (via the LLM Orchestrator, Step 5), computing hallucination
rate, P@5, MRR, nDCG@10 and latency — the metrics your proposal
specifies for RQ1 and RQ2.

**Phase A (this build):** PhoneGraph only.
**Phase B (later):** extend to also run the 5 baselines for full comparison.

---

## Prerequisites

All previous services must be running:
1. Neo4j (Docker) — port 7687
2. pgvector (Docker) — port 5432
3. Embedding microservice (Python) — port 5001
4. KG & Retrieval Service (Step 4) — port 8081
5. LLM Orchestrator (Step 5) — port 8082

---

## Setup

### 1. Copy the benchmark file into this project folder

Copy `benchmark_250_questions.json` (from Step 6) into the
`phonegraph-evaluation` folder — same level as `pom.xml`.

### 2. Build and run

```bash
mvn spring-boot:run
```

Wait for: `Started EvaluationServiceApplication`

The service runs on **http://localhost:8083**

---

## Running the evaluation

### Health check
```
http://localhost:8083/api/evaluation/health
```

### Trigger a full run (POST — takes several minutes)

```bash
curl -X POST http://localhost:8083/api/evaluation/run
```

This will:
1. Load all 250 questions
2. Send each one to the Orchestrator (which calls GLM-5.2)
3. Compute P@5, MRR, nDCG@10 against the ground truth
4. Print progress every 10 questions
5. Export a timestamped CSV to `./results/`
6. Return a JSON summary report

**Expect this to take a while** — 250 questions × a few seconds each
for GLM-5.2 generation could be 15-30+ minutes. Let it run in the
background; don't close the terminal.

---

## What the report looks like

```json
{
  "systemName": "PhoneGraph",
  "totalQuestions": 250,
  "successfulRuns": 248,
  "errorRuns": 2,
  "hallucinationRate": 0.08,
  "meanPrecisionAt5": 0.62,
  "meanReciprocalRank": 0.71,
  "meanNdcgAt10": 0.68,
  "medianLatencyMs": 2340,
  "meanLatencyMs": 2510,
  "byCategory": {
    "factual_lookup": { "count": 50, "hallucinationRate": 0.02, ... },
    "two_device_comparison": { ... },
    "recommendation_with_constraints": { ... },
    "complex_multi_attribute": { ... },
    "adversarial_typo": { ... },
    "adversarial_ambiguous": { ... },
    "adversarial_ood": { ... }
  }
}
```

The CSV in `./results/` has one row per question — this is what you
import into Excel/R/Python for your dissertation's tables, charts, and
the statistical tests (McNemar, Wilcoxon, bootstrap CI) described in
your proposal.

---

## What "done" looks like for Phase A

1. `/health` responds
2. `/run` completes without crashing (some individual question errors
   are fine and expected — they're recorded, not fatal)
3. A CSV appears in `./results/`
4. The JSON report shows real, non-zero hallucination rate and
   retrieval metrics

Once this works, you have your **first real numbers** for RQ1 — how
well PhoneGraph performs on its own before comparing to baselines.

---

## Next (Phase B)

Build 5 baseline "adapters" (B1 zero-shot, B2 BM25, B3 content-based,
B4 vanilla RAG, B5 RAG-Fusion) that each expose a compatible
`/recommend`-style endpoint, then re-run this same Evaluation Service
against each one to get the comparative RQ1/RQ2 results your
dissertation needs.
