plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.github.bmx666.appcachecleaner"

    compileSdk = 36

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
        minSdk = 23
        targetSdk = 36
        versionCode = 108
        versionName = "2.2.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
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

kotlin {
    jvmToolchain(21)
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    // stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)

    implementation(libs.google.material)

    debugImplementation(libs.timber)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Compose UI
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.material3)

    implementation(libs.compose.ui.preview)
    debugImplementation(libs.compose.ui.tooling)

    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3.window)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.runtime.rxjava2)

    implementation(libs.datastore.core)
    implementation(libs.datastore.preferences)

    implementation(libs.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.compiler)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)
}