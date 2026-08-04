package com.phonegraph.android.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface PhoneGraphApiService {

    @POST("api/orchestrator/recommend")
    suspend fun getRecommendation(
        @Body request: RecommendationRequest
    ): Response<RecommendationResponse>

    companion object {
        // 10.0.2.2 is the Android emulator's special alias for the host
        // machine's own localhost — it is NOT a typo for 127.0.0.1.
        // Routed through the Gateway (Task 3, port 8090) rather than
        // hitting the orchestrator's port 8082 directly, so the app goes
        // through the same routing layer any other client would.
        //
        // Running on a physical device instead of the emulator? Replace
        // this with your PC's actual LAN IP address (e.g. 192.168.x.x),
        // since 10.0.2.2 only resolves inside the emulator.
        private const val BASE_URL = "http://10.0.2.2:8090/"

        fun create(): PhoneGraphApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PhoneGraphApiService::class.java)
        }
    }
}
