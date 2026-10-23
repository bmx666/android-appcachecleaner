import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import com.android.apksig.SigningCertificateLineage
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifactsLoader
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    jacoco
    id("com.autonomousapps.dependency-analysis")
}

// JaCoCo 0.8.14+ is required to instrument Java 25 class files (major version 69).
val jacocoToolVersion = "0.8.14"

jacoco {
    toolVersion = jacocoToolVersion
}

// Release signing mode:
//   - public repo: release is signed by AGP with the old key AppCacheCleaner.jks only
//   - private build: the new key and lineage are present, release is signed with key rotation
//     (see RELEASE SIGNING WITH KEY ROTATION below)
val oldKeyStoreFile = layout.projectDirectory.file("AppCacheCleaner.jks")
val newKeyStoreFile = layout.projectDirectory.file("AppCacheCleaner2.jks")
val keyRotationLineageFile = layout.projectDirectory.file("AppCacheCleaner.lineage")
val isKeyRotationEnabled = newKeyStoreFile.asFile.exists() && keyRotationLineageFile.asFile.exists()
if (newKeyStoreFile.asFile.exists() != keyRotationLineageFile.asFile.exists()) {
    throw GradleException(
        "Key rotation requires both ${newKeyStoreFile.asFile.name} and ${keyRotationLineageFile.asFile.name}, " +
            "remove both to sign with ${oldKeyStoreFile.asFile.name} only."
    )
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

        // AGP injects its own JaCoCo agent into unit tests; keep it in sync with the jacoco plugin.
        testCoverage {
            jacocoVersion = jacocoToolVersion
        }

        if (!isKeyRotationEnabled) {
            signingConfigs {
                create("release") {
                    keyAlias = providers.gradleProperty("cachecleaner.oldKeyAlias").get()
                    keyPassword = providers.gradleProperty("cachecleaner.oldKeyPassword").get()
                    storeFile = oldKeyStoreFile.asFile
                    storePassword = providers.gradleProperty("cachecleaner.oldStorePassword").get()
                }
            }
        }

        defaultConfig {
            applicationId = "com.github.bmx666.appcachecleaner"
            minSdk = libs.versions.android.minSdk.get().toInt()

            versionCode = 110
            versionName = "2.3.1"

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
                if (!isKeyRotationEnabled) {
                    signingConfig = signingConfigs.getByName("release")
                }
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
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
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

    jvmToolchain(25)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

// =========================================================================
// RELEASE SIGNING WITH KEY ROTATION (APK Signature Scheme v3 lineage)
// =========================================================================
// AGP signingConfigs cannot sign with a lineage, so the release build type stays unsigned and
// this task signs the unsigned APK with both keys:
//   - v2 (API 24-27): old key AppCacheCleaner.jks
//   - v3 (API 28+):   new key AppCacheCleaner2.jks + AppCacheCleaner.lineage (old -> new)
// Output: build/outputs/apk/release-signed/app-release.apk
// Enabled only when AppCacheCleaner2.jks and AppCacheCleaner.lineage are present.
if (isKeyRotationEnabled) androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val signTask = tasks.register<SignApkWithLineageTask>("sign${variant.name.replaceFirstChar { it.uppercase() }}ApkWithLineage") {
            group = "build"
            description = "Signs the ${variant.name} APK with the old and new keys using the rotation lineage."

            apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            lineageFile.set(keyRotationLineageFile)
            rotationMinSdkVersion.set(28)

            oldStoreFile.set(oldKeyStoreFile)
            oldStorePassword.set(providers.gradleProperty("cachecleaner.oldStorePassword"))
            oldKeyAlias.set(providers.gradleProperty("cachecleaner.oldKeyAlias"))
            oldKeyPassword.set(providers.gradleProperty("cachecleaner.oldKeyPassword"))

            newStoreFile.set(newKeyStoreFile)
            newStorePassword.set(providers.gradleProperty("cachecleaner.storePassword"))
            newKeyAlias.set(providers.gradleProperty("cachecleaner.keyAlias"))
            newKeyPassword.set(providers.gradleProperty("cachecleaner.keyPassword"))

            outputApk.set(layout.buildDirectory.file("outputs/apk/${variant.name}-signed/${project.name}-${variant.name}.apk"))
        }
        val assembleTaskName = "assemble${variant.name.replaceFirstChar { it.uppercase() }}"
        tasks.named { it == assembleTaskName }.configureEach { finalizedBy(signTask) }
    }
}

abstract class SignApkWithLineageTask : DefaultTask() {
    @get:InputFiles
    abstract val apkFolder: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:InputFile
    abstract val lineageFile: RegularFileProperty

    @get:Input
    abstract val rotationMinSdkVersion: Property<Int>

    @get:InputFile
    abstract val oldStoreFile: RegularFileProperty

    @get:Internal
    abstract val oldStorePassword: Property<String>

    @get:Input
    abstract val oldKeyAlias: Property<String>

    @get:Internal
    abstract val oldKeyPassword: Property<String>

    @get:InputFile
    abstract val newStoreFile: RegularFileProperty

    @get:Internal
    abstract val newStorePassword: Property<String>

    @get:Input
    abstract val newKeyAlias: Property<String>

    @get:Internal
    abstract val newKeyPassword: Property<String>

    @get:OutputFile
    abstract val outputApk: RegularFileProperty

    @TaskAction
    fun sign() {
        val builtArtifacts = builtArtifactsLoader.get().load(apkFolder.get())
            ?: throw GradleException("Cannot load APK metadata from ${apkFolder.get()}")
        val inputApk = File(builtArtifacts.elements.single().outputFile)
        val outputFile = outputApk.get().asFile

        // Signer order must follow the lineage: oldest first.
        val signers = listOf(
            signerConfig("CERT", oldStoreFile, oldStorePassword, oldKeyAlias, oldKeyPassword),
            signerConfig("CERT2", newStoreFile, newStorePassword, newKeyAlias, newKeyPassword),
        )

        ApkSigner.Builder(signers)
            .setInputApk(inputApk)
            .setOutputApk(outputFile)
            .setSigningCertificateLineage(SigningCertificateLineage.readFromFile(lineageFile.get().asFile))
            .setMinSdkVersionForRotation(rotationMinSdkVersion.get())
            .build()
            .sign()

        val result = ApkVerifier.Builder(outputFile).build().verify()
        if (!result.isVerified) {
            throw GradleException("Signed APK failed verification: ${result.allErrors.joinToString()}")
        }
        logger.lifecycle("Signed APK with lineage: $outputFile")
    }

    private fun signerConfig(
        name: String,
        storeFile: RegularFileProperty,
        storePassword: Property<String>,
        keyAlias: Property<String>,
        keyPassword: Property<String>,
    ): ApkSigner.SignerConfig {
        val keyStore = KeyStore.getInstance(storeFile.get().asFile, storePassword.get().toCharArray())
        val privateKey = keyStore.getKey(keyAlias.get(), keyPassword.get().toCharArray()) as PrivateKey
        val certificates = keyStore.getCertificateChain(keyAlias.get()).map { it as X509Certificate }
        return ApkSigner.SignerConfig.Builder(name, privateKey, certificates).build()
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