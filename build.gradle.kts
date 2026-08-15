plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.dependency.analysis)
}

buildscript {
    dependencies {
        classpath(libs.hilt.gradle.plugin)
    }
}

dependencyAnalysis {
    issues {
        // Report advice instead of failing the build. `buildHealth` defaults to `fail`, which stops
        // at the first finding and hides the rest of the report; `warn` prints the whole thing.
        // Raise this back to "fail" once the reported advice has been worked through.
        all {
            onAny { severity("warn") }
        }
    }
}