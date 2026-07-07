plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "io.getunleash.openfeature.android.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.getunleash.openfeature.android.example"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-alpha.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":provider"))
    implementation(libs.androidx.core.ktx)
}
