package com.phonegraph.android.data

/**
 * Mirrors RecommendationRequest.java on the backend exactly (field names
 * and defaults), so Gson serialises this into a body the orchestrator
 * already understands — no backend changes needed for the Android client.
 */
data class RecommendationRequest(
    val query: String,
    val brand: String? = null,
    val maxPrice: Int? = null,
    val limit: Int = 5
)
