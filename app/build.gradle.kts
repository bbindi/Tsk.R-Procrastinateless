import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("release-signing.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun signingProperty(name: String): String? =
    releaseSigningProperties.getProperty(name)
        ?: providers.gradleProperty(name).orNull
        ?: providers.environmentVariable("TSKR_${name.uppercase()}").orNull

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.brixavier.tskr"
    compileSdk = 37

    val sampleTestAppId = "ca-app-pub-3940256099942544~3347511713"
    val sampleTestAdUnitId = "ca-app-pub-3940256099942544/2247696110"

    val prodAppIdDefault = "ca-app-pub-1968698168446413~5409238684"
    val prodAdUnitIdDefault = "ca-app-pub-1968698168446413/7791859443"

    val activeProdAppId = signingProperty("adMobAppId") ?: prodAppIdDefault
    val activeProdAdUnitId = signingProperty("adMobNativeAdUnitId") ?: prodAdUnitIdDefault

    defaultConfig {
        applicationId = "com.brixavier.tskr"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "2.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            // Debug build variant strictly uses Google sample test AdMob IDs
            manifestPlaceholders["adMobAppId"] = sampleTestAppId
            buildConfigField("String", "ADMOB_NATIVE_AD_UNIT_ID", "\"$sampleTestAdUnitId\"")
        }

        release {
            val storeFilePath = signingProperty("storeFile")
            val storePassword = signingProperty("storePassword")
            val keyAlias = signingProperty("keyAlias")
            val keyPassword = signingProperty("keyPassword")

            check(
                listOf(storeFilePath, storePassword, keyAlias, keyPassword).all { !it.isNullOrBlank() }
            ) {
                "Release signing credentials are missing. Supply storeFile, storePassword, keyAlias, and keyPassword " +
                    "in ignored release-signing.properties, Gradle properties, or TSKR_* environment variables."
            }

            signingConfig = signingConfigs.create("release") {
                storeFile = rootProject.file(storeFilePath!!)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }

            // Release build variant uses production Tsk.R AdMob IDs
            manifestPlaceholders["adMobAppId"] = activeProdAppId
            buildConfigField("String", "ADMOB_NATIVE_AD_UNIT_ID", "\"$activeProdAdUnitId\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register("validateProductionAdMobIds") {
    group = "verification"
    description = "Strictly validates that production AdMob Application ID and Native Ad Unit ID match Tsk.R production IDs for release."

    doLast {
        val prodAppIdExpected = "ca-app-pub-1968698168446413~5409238684"
        val prodAdUnitIdExpected = "ca-app-pub-1968698168446413/7791859443"

        val sampleTestAppId = "ca-app-pub-3940256099942544~3347511713"
        val sampleTestAdUnitId = "ca-app-pub-3940256099942544/2247696110"

        val activeAppId = signingProperty("adMobAppId") ?: prodAppIdExpected
        val activeAdUnitId = signingProperty("adMobNativeAdUnitId") ?: prodAdUnitIdExpected

        val appIdPattern = Regex("^ca-app-pub-\\d{16}~\\d{10}$")
        val unitIdPattern = Regex("^ca-app-pub-\\d{16}/\\d{10}$")

        if (activeAppId == sampleTestAppId || !appIdPattern.matches(activeAppId) || activeAppId != prodAppIdExpected) {
            throw GradleException(
                "PRODUCTION ADMOB ID VALIDATION FAILED: Production adMobAppId is invalid or set to test ID. Expected '$prodAppIdExpected'."
            )
        }

        if (activeAdUnitId == sampleTestAdUnitId || !unitIdPattern.matches(activeAdUnitId) || activeAdUnitId != prodAdUnitIdExpected) {
            throw GradleException(
                "PRODUCTION ADMOB ID VALIDATION FAILED: Production adMobNativeAdUnitId is invalid or set to test ID. Expected '$prodAdUnitIdExpected'."
            )
        }

        logger.lifecycle("SUCCESS: Production AdMob IDs (App ID: $prodAppIdExpected, Native Ad Unit ID: $prodAdUnitIdExpected) are valid and configured for release.")
    }
}

dependencies {
    // Jetpack Glance (Widgets)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Compose Rich Editor (Mohamed Rejeb)
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.2.0")

    // WorkManager (Background Sync)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Google Play In-App Updates & Reviews
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Google Mobile Ads (AdMob) & User Messaging Platform (UMP) Consent SDK
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    // Jetpack Compose BOM & Core
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    debugImplementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-shapes:1.0.1")

    // Lifecycle and ViewModel (with Compose integration)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Room Database
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines (explicit support)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Android Core & Standard Testing Libraries
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
