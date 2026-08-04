package com.phonegraph.android.data

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
                RecommendationResult.Error(
                    "Server returned an error (code ${response.code()}). Is the backend running?"
                )
            }
        } catch (e: IOException) {
            RecommendationResult.Error(
                "Could not reach PhoneGraph. Check the backend services and gateway are running."
            )
        } catch (e: Exception) {
            RecommendationResult.Error("Unexpected error: ${e.message}")
        }
    }
}
