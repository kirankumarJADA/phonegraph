package com.phonegraph.android.data

import android.util.Log
import java.io.IOException

/**
 * Result of asking PhoneGraph for a recommendation. A sealed type so the
 * ViewModel/UI can exhaustively handle both outcomes without dealing with
 * raw exceptions or HTTP response codes directly.
 */
sealed class RecommendationResult {
    data class Success(val response: RecommendationResponse) : RecommendationResult()
    data class Error(val message: String) : RecommendationResult()
}

/**
 * PhoneGraphRepository — the single place that knows how to talk to the
 * backend. The ViewModel depends on this, not on Retrofit directly, so
 * the networking library could be swapped later without touching UI code.
 */
class PhoneGraphRepository(
    private val api: PhoneGraphApiService = PhoneGraphApiService.create()
) {
    suspend fun getRecommendation(query: String): RecommendationResult {
        return try {
            val response = api.getRecommendation(RecommendationRequest(query = query))
            if (response.isSuccessful && response.body() != null) {
                RecommendationResult.Success(response.body()!!)
            } else {
                Log.e("PhoneGraphRepository", "Server error: HTTP ${response.code()} — ${response.errorBody()?.string()}")
                RecommendationResult.Error(
                    "Server returned an error (code ${response.code()}). Is the backend running?"
                )
            }
        } catch (e: IOException) {
            // Logged with the full exception so the REAL cause (timeout,
            // connection refused, unknown host, etc.) is visible in
            // Logcat — the UI message stays generic on purpose, but this
            // line is what makes the actual bug diagnosable.
            Log.e("PhoneGraphRepository", "Network error calling PhoneGraph", e)
            RecommendationResult.Error(
                "Could not reach PhoneGraph (${e.javaClass.simpleName}: ${e.message}). " +
                        "Check the backend services and gateway are running."
            )
        } catch (e: Exception) {
            Log.e("PhoneGraphRepository", "Unexpected error calling PhoneGraph", e)
            RecommendationResult.Error("Unexpected error: ${e.message}")
        }
    }
}
