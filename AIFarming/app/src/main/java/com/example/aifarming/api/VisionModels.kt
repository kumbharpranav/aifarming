package com.example.aifarming.api

// Request models for Cloud Vision API
data class VisionRequest(
    val requests: List<ImageRequest>
)

data class ImageRequest(
    val image: ImageContent,
    val features: List<Feature>
)

data class ImageContent(
    val content: String  // Base64 encoded image string
)

data class Feature(
    val type: String,
    val maxResults: Int
)

// Response models for Cloud Vision API
data class VisionResponse(
    val responses: List<ImageResponse>
)

data class ImageResponse(
    val safeSearchAnnotation: SafeSearchAnnotation?
)

data class SafeSearchAnnotation(
    val adult: String?,
    val spoof: String?,
    val medical: String?,
    val violence: String?,
    val racy: String?
)
