plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    id("com.autonomousapps.dependency-analysis") version "3.14.1"
}

allprojects {
    apply(plugin = "com.autonomousapps.dependency-analysis")
}

buildscript {
    dependencies {
        classpath(libs.hilt.gradle.plugin)
    }
}