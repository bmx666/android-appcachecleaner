import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
}

kotlin {
    android {
        namespace = "com.github.bmx666.appcachecleaner"
        compileSdk {
            version = release(libs.versions.android.compileSdk.version.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdk.minorApiLevel.get().toInt()
            }
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

            versionCode = 108
            versionName = "2.2.10"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        buildTypes {
            release {
                isDebuggable = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.getByName("release")
            }
            debug {
                isDebuggable = true
                applicationIdSuffix = ".debug"
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)

    // Compose UI
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

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
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)
}