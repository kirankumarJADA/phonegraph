package com.phonegraph.orchestrator.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HallucinationChecker {

private static final Set<String> PHRASE_STOPWORDS = Set.of(
            "Best", "Top", "Honest", "Based", "According", "However",
            "Note", "Important", "Overall", "In", "For", "The", "This",
            "What", "Why", "How", "Here", "Given", "Since", "While",
            "Recommendation", "Summary", "Conclusion", "Answer", "Pick",
            "Choice", "Option", "Result", "Verdict", "Ranked",
            "Same", "Solid", "Strong", "Good", "Great", "Similar",
            "Higher", "Lower", "Larger", "Smaller", "Better", "Worse"
    );

    private static final Set<String> KNOWN_BRAND_WORDS = Set.of(
            "Samsung", "Apple", "iPhone", "Galaxy", "Xiaomi", "Redmi",
            "OnePlus", "Google", "Pixel", "Sony", "Xperia", "Motorola",
            "Moto", "Nokia", "Oppo", "Vivo", "Realme", "Huawei", "Honor",
            "Nothing", "Asus", "ROG", "Fairphone", "TCL", "ZTE", "Meizu",
            "iQOO", "Infinix", "Tecno", "HTC", "Nubia", "Blackview"
    );

    public HallucinationResult check(String llmResponse, List<Map<String, Object>> candidatePhones) {
        List<String> allowedNames = new ArrayList<>();

        for (Map<String, Object> phone : candidatePhones) {
            String name = String.valueOf(phone.getOrDefault("name", phone.getOrDefault("phoneName", "")));
            if (!name.isBlank()) {
                allowedNames.add(name);
            }

            Object chipset = phone.get("chipset");
            if (chipset instanceof Map) {
                Object chipsetName = ((Map<?, ?>) chipset).get("name");
                if (chipsetName != null) {
                    allowedNames.add(String.valueOf(chipsetName));
                }
            } else if (chipset instanceof String) {
                allowedNames.add((String) chipset);
            }

            Object display = phone.get("display");
            if (display instanceof Map) {
                Object displayType = ((Map<?, ?>) display).get("type");
                if (displayType != null) {
                    allowedNames.add(String.valueOf(displayType));
                }
            }
        }

        List<String> flaggedClaims = new ArrayList<>();

        Pattern phoneNamePattern = Pattern.compile(
                "([A-Z][a-zA-Z0-9]+(?:\\s[A-Z0-9][a-zA-Z0-9+]*){1,4})"
        );
        Matcher matcher = phoneNamePattern.matcher(llmResponse);

        while (matcher.find()) {
            String candidate = matcher.group(1).trim();

            if (candidate.split("\\s").length < 2 || candidate.length() <= 6) {
                continue;
            }

            String firstWord = candidate.split("\\s")[0];
            if (PHRASE_STOPWORDS.contains(firstWord)) {
                continue;
            }

            boolean containsDigit = candidate.chars().anyMatch(Character::isDigit);
            boolean containsBrand = KNOWN_BRAND_WORDS.stream().anyMatch(candidate::contains);

            if (!containsDigit && !containsBrand) {
                continue;
            }

            boolean isAllowed = allowedNames.stream()
                    .anyMatch(allowed -> allowed.contains(candidate)
                            || candidate.contains(allowed)
                            || candidate.startsWith(allowed));

            if (!isAllowed && !flaggedClaims.contains(candidate)) {
                flaggedClaims.add(candidate);
            }
        }

        return new HallucinationResult(!flaggedClaims.isEmpty(), flaggedClaims);
    }

    public record HallucinationResult(boolean hallucinationDetected, List<String> flaggedClaims) {}
}