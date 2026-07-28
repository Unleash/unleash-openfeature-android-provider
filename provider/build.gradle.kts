plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.spotless)
    `maven-publish`
}

android {
    namespace = "io.getunleash.openfeature.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_FLAVOUR_VERSION", "\"${project.version}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.openfeature.android.sdk)
    api(libs.unleash.android)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "unleash-openfeature-android-provider"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("io.getunleash:unleash-openfeature-android-provider")
                description.set("Android provider for Unleash and OpenFeature.")
                url.set("https://github.com/Unleash/unleash-openfeature-android-provider")
                inceptionYear.set("2026")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("simonhornby")
                        name.set("Simon Hornby")
                        email.set("simon@getunleash.io")
                        organization.set("Unleash")
                        organizationUrl.set("https://getunleash.io")
                    }
                }

                scm {
                    url.set("https://github.com/Unleash/unleash-openfeature-android-provider")
                    connection.set("scm:git:https://github.com/Unleash/unleash-openfeature-android-provider.git")
                    developerConnection.set("scm:git:https://github.com/Unleash/unleash-openfeature-android-provider.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "staging"
            url =
                uri(
                    layout.buildDirectory
                        .dir("staging-deploy")
                        .get()
                        .asFile,
                )
        }
    }
}
