plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.brendigo.vojs"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.brendigo.vojs"
        minSdk = 24
        targetSdk = 35
        versionCode = (providers.gradleProperty("vojsVersionCode").orNull?.toIntOrNull() ?: 1)
        versionName = providers.gradleProperty("vojsVersionName").orNull ?: "1.0.0"
    }
    val canSign = listOf("VOJS_KEYSTORE_PATH", "VOJS_STORE_PASSWORD", "VOJS_KEY_ALIAS", "VOJS_KEY_PASSWORD")
        .all { !System.getenv(it).isNullOrBlank() }
    signingConfigs {
        if (canSign) {
            create("vojsProduction") {
                storeFile = file(System.getenv("VOJS_KEYSTORE_PATH"))
                storePassword = System.getenv("VOJS_STORE_PASSWORD")
                keyAlias = System.getenv("VOJS_KEY_ALIAS")
                keyPassword = System.getenv("VOJS_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (canSign) signingConfig = signingConfigs.getByName("vojsProduction")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(bom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.core:core-ktx:1.15.0")
}
