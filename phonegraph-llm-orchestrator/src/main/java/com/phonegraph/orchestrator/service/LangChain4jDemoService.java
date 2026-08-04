package com.phonegraph.orchestrator.service;

import com.phonegraph.orchestrator.model.KgRetrievalResult;
import com.phonegraph.orchestrator.model.RecommendationResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * LangChain4jDemoService — a genuine, isolated LangChain4j integration.
 *
 * The proposal names LangChain4j for orchestration, but the actual working
 * pipeline (NvidiaLlmClient, used by PhoneGraph and all already-evaluated
 * baselines/ablations) makes raw HTTP calls instead, built and debugged
 * that way over many hours tonight. Rewriting that shared client this late
 * risks the results already collected, which the project's own rules say
 * not to do.
 *
 * This class instead demonstrates LangChain4j genuinely and separately:
 * it uses dev.langchain4j's own ChatLanguageModel abstraction (via its
 * OpenAI-compatible client, pointed at NVIDIA NIM, which exposes an
 * OpenAI-compatible API) to run the exact same retrieval + constrained
 * prompt as PhoneGraph. It does not touch NvidiaLlmClient, BaselineService's
 * other methods, or any endpoint already used to produce evaluation data.
 */
@Service
public class LangChain4jDemoService {

    private final ChatLanguageModel chatModel;
    private final KgRetrievalClient kgRetrievalClient;
    private final HallucinationChecker hallucinationChecker;
    private final PromptBuilder promptBuilder;

    public LangChain4jDemoService(
            @Value("${phonegraph.nvidia.api-key}") String apiKey,
            @Value("${phonegraph.nvidia.base-url:https://integrate.api.nvidia.com/v1}") String baseUrl,
            @Value("${phonegraph.nvidia.model:z-ai/glm-5.2}") String model,
            KgRetrievalClient kgRetrievalClient,
            HallucinationChecker hallucinationChecker,
            PromptBuilder promptBuilder) {

        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.2)
                .maxTokens(400)
                .timeout(Duration.ofSeconds(300))
                .maxRetries(3)
                .build();

        this.kgRetrievalClient = kgRetrievalClient;
        this.hallucinationChecker = hallucinationChecker;
        this.promptBuilder = promptBuilder;
    }

    public RecommendationResponse recommend(String query) {
        KgRetrievalResult retrieval = kgRetrievalClient.hybridSearch(query, null, null, 10);
        List<Map<String, Object>> candidates = BaselineService.buildFusedCandidates(retrieval);

        if (candidates.isEmpty()) {
            RecommendationResponse resp = new RecommendationResponse();
            resp.setAnswer("No phones found.");
            resp.setCandidatePhones(Collections.emptyList());
            resp.setHallucinationDetected(false);
            resp.setFlaggedClaims(Collections.emptyList());
            resp.setModelUsed("langchain4j-demo");
            return resp;
        }

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(query, candidates);

        List<ChatMessage> messages = List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
        );

        // The actual call goes through LangChain4j's ChatLanguageModel
        // interface, not NvidiaLlmClient's raw HttpURLConnection code.
        Response<AiMessage> response = chatModel.generate(messages);
        String answer = response.content().text();

        var hallCheck = hallucinationChecker.check(answer, candidates);

        RecommendationResponse resp = new RecommendationResponse();
        resp.setAnswer(answer);
        resp.setCandidatePhones(BaselineService.extractNames(candidates));
        resp.setHallucinationDetected(hallCheck.hallucinationDetected());
        resp.setFlaggedClaims(hallCheck.flaggedClaims());
        resp.setModelUsed("langchain4j-demo");
        return resp;
    }
}
