package com.example.aifarming.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GeminiService {
    @POST("v1/generate")
    suspend fun generateResponse(@Body prompt: GeminiPrompt): Response<GeminiResponse>
}

data class GeminiPrompt(
    val text: String
)

data class GeminiResponse(
    val generatedText: String
)
