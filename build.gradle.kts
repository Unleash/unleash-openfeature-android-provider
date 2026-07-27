plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.jreleaser)
}

allprojects {
    group = "io.getunleash"
    version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")
}

jreleaser {
    configFile.set(layout.projectDirectory.file("jreleaser.yml"))
}
