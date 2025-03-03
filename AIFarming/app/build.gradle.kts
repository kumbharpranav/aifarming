import org.gradle.api.JavaVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.aifarming"
    compileSdk = 35

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        applicationId = "com.example.aifarming"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Custom BuildConfig fields
        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("geminiApiKey") as String? ?: "undefined"}\"")
        buildConfigField("String", "VISION_API_KEY", "\"${project.findProperty("visionApiKey") as String? ?: "undefined"}\"")
    }

    buildFeatures {
        // Enable BuildConfig generation
        buildConfig = true
        // Also enable Compose if you are using it
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    compileOptions {
        targetCompatibility = VERSION_11
    }
    buildToolsVersion = "35.0.0"
}


dependencies {
    // Core Android libraries

    implementation(libs.androidx.core.ktx.v190)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material3)
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)
    implementation(libs.androidx.lifecycle.runtime.ktx.v287)
    implementation(libs.androidx.activity.compose.v171)
    // Jetpack Compose dependencies
    implementation(libs.androidx.ui.v143)
    implementation(libs.androidx.material)
    implementation(libs.androidx.ui.tooling.preview.v143)
    debugImplementation(libs.androidx.ui.tooling.v143)
    implementation(libs.androidx.lifecycle.runtime.ktx.v287)
    implementation(libs.androidx.activity.compose.v1101)

    // Firebase dependencies for authentication and Realtime Database
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.play.services.auth)

    // Retrofit for API integration
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
}
