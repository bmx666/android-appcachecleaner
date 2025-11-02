plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    id("com.autonomousapps.dependency-analysis") version "3.4.1"
}

allprojects {
    apply(plugin = "com.autonomousapps.dependency-analysis")
}

buildscript {
    dependencies {
        classpath(libs.hilt.gradle.plugin)
    }
}