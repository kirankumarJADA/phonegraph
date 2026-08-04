package com.phonegraph.android.data

/**
 * Mirrors RecommendationResponse.java on the backend exactly.
 */
data class RecommendationResponse(
    val answer: String,
    val candidatePhones: List<String> = emptyList(),
    val flaggedClaims: List<String> = emptyList(),
    val hallucinationDetected: Boolean = false,
    val modelUsed: String? = null
)
