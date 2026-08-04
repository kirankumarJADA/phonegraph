package com.phonegraph.orchestrator.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * KgRetrievalResult — mirrors the response shape returned by the
 * KG & Retrieval Service's /api/retrieval/hybrid endpoint (built in
 * Step 4). Kept intentionally loose (Map-based) since we only need to
 * read specific fields, not fully model that service's internal types.
 */
@Data
public class KgRetrievalResult {
    private List<Map<String, Object>> graphMatches;
    private List<Map<String, Object>> semanticMatches;
    private List<String> fusedRanking;
}
