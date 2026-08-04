package com.phonegraph.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PhoneGraph — LLM Orchestrator Service
 * =======================================
 * One of the 4 microservices in the PhoneGraph architecture.
 *
 * Responsibilities:
 *  - Call the KG & Retrieval Service (Step 4) to get candidate phones
 *  - Build a constrained prompt containing ONLY KG-verified facts
 *  - Send the prompt to GLM-5.2 (via NVIDIA's API)
 *  - Check every claim in the LLM's response against the KG whitelist
 *  - Strip or flag any hallucinated values before returning to the user
 *
 * This is the core hallucination-prevention mechanism described in the
 * PhoneGraph proposal (Section 3.6, Figure 1 Layer 3).
 */
@SpringBootApplication
public class LlmOrchestratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmOrchestratorServiceApplication.class, args);
    }
}
