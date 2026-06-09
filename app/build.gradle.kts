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

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)

    implementation(libs.google.material)

    debugImplementation(libs.timber)

    // --- unit tests (JVM + Robolectric) ---
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.dagger.hilt.compiler)

    // --- instrumented + Compose UI tests ---
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    // UiAutomator: drives the SYSTEM App Info / Settings screens the accessibility
    // service walks (cross-process) - Espresso cannot leave the app under test.
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.dagger.hilt.compiler)

    // Compose UI
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    // Robolectric-driven Compose smoke tests run in the JVM unit-test source set.
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)

    implementation(libs.compose.material3)

    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)

    implementation(libs.datastore.core)
    implementation(libs.datastore.preferences)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.compiler)
    ksp(libs.dagger.hilt.compiler)
    // Upgrade metadata lib on KSP classpath so Dagger reads Kotlin 2.3 metadata (v2.4.0).
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)
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