import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

kotlin {
    android {
        namespace = "com.github.bmx666.appcachecleaner"
        compileSdk {
            version = release(libs.versions.android.compileSdk.version.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdk.minorApiLevel.get().toInt()
            }
        }

        dependenciesInfo {
            // Disables dependency metadata when building APKs.
            includeInApk = false
            // Disables dependency metadata when building Android App bundles.
            includeInBundle = false
        }

        signingConfigs {
            create("release") {
                keyAlias = "AppCacheCleaner"
                keyPassword = "AppCacheCleaner"
                storeFile = file("AppCacheCleaner.jks")
                storePassword = "AppCacheCleaner"
            }
        }

        defaultConfig {
            applicationId = "com.github.bmx666.appcachecleaner"
            minSdk = libs.versions.android.minSdk.get().toInt()

            versionCode = 109
            versionName = "2.3.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        buildTypes {
            release {
                isDebuggable = false
                isShrinkResources = true
                isMinifyEnabled = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.getByName("release")
            }
            debug {
                isDebuggable = true
                applicationIdSuffix = ".debug"
                // Emit JaCoCo exec data from JVM unit tests (report-only; no gate).
                enableUnitTestCoverage = true
            }
        }
        bundle {
            storeArchive {
                enable = false
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
        buildFeatures {
            buildConfig = true
            compose = true
            viewBinding = true
        }
        testOptions {
            unitTests {
                // Robolectric needs merged resources/manifest; default-value stubs keep
                // un-shadowed android.* calls from throwing in plain (non-Robolectric) tests.
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
            }
        }
    }

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    // stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

@Suppress("DuplicatePlatformDeclaration")
dependencies {
    // =========================================================================
    // VERSION ALIGNMENT (Bill of Materials)
    // =========================================================================
    // Enforces consistent, mutually compatible versions across all Compose modules
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    // =========================================================================
    // CORE APPLICATION FRAMEWORK (Bundles)
    // =========================================================================
    // Primary Jetpack Compose runtime UI dependencies, Navigation, and Image Loading
    implementation(libs.bundles.compose.core)
    // Local data storage management engine
    implementation(libs.bundles.datastore)

    // =========================================================================
    // COMPONENT EXTENSIONS & TRANSITIONAL UI
    // =========================================================================
    // Required to prevent resource linking crashes by providing legacy XML MaterialComponents themes
    implementation(libs.google.material)
    // Core foundational utilities & annotations
    implementation(libs.androidx.annotation)
    // Accompanist extension wrapper for runtime permission workflows in Compose
    implementation(libs.accompanist.permissions)
    // Supplemental material icon catalog (Keep only if custom extended icons are explicitly requested)
    implementation(libs.compose.material.icons.extended)

    // =========================================================================
    // DEPENDENCY INJECTION (Dagger Hilt)
    // =========================================================================
    implementation(libs.dagger.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // =========================================================================
    // METADATA & CODE GENERATION PROCESSORS (KSP)
    // =========================================================================
    ksp(libs.dagger.compiler)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    kspTest(libs.dagger.hilt.compiler)
    kspAndroidTest(libs.dagger.hilt.compiler)

    // =========================================================================
    // DEVELOPMENT & DIAGNOSTIC UTILITIES
    // =========================================================================
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.timber)
    debugRuntimeOnly(libs.compose.ui.test.manifest)

    // =========================================================================
    // LOCAL UNIT TESTING (JVM / Robolectric Stack)
    // =========================================================================
    testImplementation(libs.bundles.test.jvm)
    testImplementation(libs.compose.ui.test.junit4)

    // =========================================================================
    // INSTRUMENTED TESTING (On-Device Stack)
    // =========================================================================
    androidTestImplementation(libs.bundles.test.instrumented)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestRuntimeOnly(libs.androidx.test.core)
}

// Report-only coverage (no enforced threshold). Run: ./gradlew jacocoTestReport
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "JaCoCo coverage report for debug JVM unit tests (report-only)."

    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    // Exclude generated / framework / UI-only code from the denominator so the number
    // reflects hand-written logic, which is what the test suite targets.
    val excludes = listOf(
        "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        "**/*_Factory*.*", "**/*_*Factory.*", "**/*_HiltModules*.*", "**/Hilt_*.*",
        "**/*_MembersInjector*.*", "**/*_Impl*.*", "**/Dagger*.*",
        "**/*ComposableSingletons*.*", "**/ComposableSingletons*.*",
        "org/springframework/**",
        "**/ui/theme/**", "**/ui/compose/view/**",
    )

    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(excludes)
    }
    val javaClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
        exclude(excludes)
    }
    classDirectories.setFrom(kotlinClasses, javaClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec",
            )
        }
    )
}