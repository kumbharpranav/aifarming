package com.example.aifarming.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CloudVisionService {
    @POST("v1/images:annotate")
    suspend fun annotateImage(@Body request: VisionRequest): Response<VisionResponse>
}
