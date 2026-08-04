package com.phonegraph.orchestrator.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
        You are PhoneGraph, a smartphone recommendation assistant.

        ABSOLUTE RULES — violating ANY of these makes your answer WRONG:

        1. You may ONLY mention phones from the CANDIDATE PHONES list below.
           If a phone name is NOT in the list, you MUST NOT mention it.
        2. You may ONLY state specifications that are explicitly written in
           the CANDIDATE PHONES data. If a spec is missing, say "not specified".
           NEVER guess, infer, or use your training knowledge for specs.
        3. NEVER invent or fabricate phone names, model numbers, prices,
           specs, or features. Every fact in your answer must come from
           the CANDIDATE PHONES data provided.
        4. Use the EXACT phone names as written in the list — do not
           shorten, abbreviate, or modify them.
        5. If the user asks about a phone not in the list, say
           "That phone is not in my current database."
        6. Keep your answer concise: a ranked list with short reasons,
           referencing only the given data.

        REMEMBER: Your ONLY source of truth is the CANDIDATE PHONES list.
        Anything not in that list does not exist for you.
        """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(String userQuery, List<Map<String, Object>> candidatePhones) {
        StringBuilder sb = new StringBuilder();
        sb.append("USER REQUEST: ").append(userQuery).append("\n\n");
        sb.append("CANDIDATE PHONES (use ONLY this data):\n");

        int i = 1;
        for (Map<String, Object> phone : candidatePhones) {
            sb.append(i++).append(". ");
            sb.append("Name: ").append(phone.getOrDefault("name", phone.getOrDefault("phoneName", "unknown"))).append(" | ");
            sb.append("Brand: ").append(phone.getOrDefault("brand", "not specified")).append(" | ");
            sb.append("Price: ").append(phone.getOrDefault("price", "not specified")).append(" | ");
            sb.append("RAM: ").append(phone.getOrDefault("ram", "not specified")).append(" | ");
            sb.append("Storage: ").append(phone.getOrDefault("storage", "not specified")).append(" | ");
            sb.append("Chipset: ").append(extractField(phone.get("chipset"), "name")).append(" | ");
            sb.append("Display: ").append(extractField(phone.get("display"), "type", "sizeInches")).append(" | ");
            sb.append("Battery: ").append(extractField(phone.get("battery"), "capacityMah")).append(" | ");
            sb.append("Camera: ").append(extractField(phone.get("camera"), "mainCamera"));
            sb.append("\n");
        }

        sb.append("\nBased ONLY on the phones listed above, recommend the best match(es) for the user's request.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractField(Object nested, String... fieldNames) {
        if (nested == null) {
            return "not specified";
        }
        if (!(nested instanceof Map)) {
            return String.valueOf(nested);
        }
        Map<String, Object> map = (Map<String, Object>) nested;
        StringBuilder result = new StringBuilder();
        for (String field : fieldNames) {
            Object value = map.get(field);
            if (value != null && !"N/A".equals(value)) {
                if (result.length() > 0) result.append(" ");
                result.append(value);
            }
        }
        return result.length() > 0 ? result.toString() : "not specified";
    }
}