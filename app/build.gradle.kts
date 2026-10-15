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

    defaultConfig {
        applicationId = "com.brixavier.tskr"
        minSdk = 26
        targetSdk = 37
        versionCode = 7
        versionName = "2.1.0"

        val testAppId = "ca-app-pub-3940256099942544~3347511713"
        val testAdUnitId = "ca-app-pub-3940256099942544/2247696110"

        val rawAdMobAppId = signingProperty("adMobAppId")
        val rawAdMobNativeAdUnitId = signingProperty("adMobNativeAdUnitId")

        val adMobAppId = if (!rawAdMobAppId.isNullOrBlank()) {
            val appIdPattern = Regex("^ca-app-pub-\\d{16}~\\d{10}$")
            if (rawAdMobAppId != testAppId && !appIdPattern.matches(rawAdMobAppId)) {
                logger.warn("WARNING: adMobAppId format check failed for provided custom property.")
            }
            rawAdMobAppId
        } else {
            testAppId
        }

        val adMobNativeAdUnitId = if (!rawAdMobNativeAdUnitId.isNullOrBlank()) {
            val unitIdPattern = Regex("^ca-app-pub-\\d{16}/\\d{10}$")
            if (rawAdMobNativeAdUnitId != testAdUnitId && !unitIdPattern.matches(rawAdMobNativeAdUnitId)) {
                logger.warn("WARNING: adMobNativeAdUnitId format check failed for provided custom property.")
            }
            rawAdMobNativeAdUnitId
        } else {
            testAdUnitId
        }

        if (adMobAppId == testAppId || adMobNativeAdUnitId == testAdUnitId) {
            logger.warn("\n[ADMOB BUILD WARNING] Build is using Google Sample Test AdMob IDs. Supply production adMobAppId and adMobNativeAdUnitId in release-signing.properties or environment variables prior to Google Play Store submission.\n")
        }

        manifestPlaceholders["adMobAppId"] = adMobAppId
        buildConfigField("String", "ADMOB_NATIVE_AD_UNIT_ID", "\"$adMobNativeAdUnitId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
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
    description = "Strictly validates that production AdMob Application ID and Native Ad Unit ID are configured and valid for production release."

    doLast {
        val testAppId = "ca-app-pub-3940256099942544~3347511713"
        val testAdUnitId = "ca-app-pub-3940256099942544/2247696110"

        val rawAppId = signingProperty("adMobAppId")
        val rawAdUnitId = signingProperty("adMobNativeAdUnitId")

        val appIdPattern = Regex("^ca-app-pub-\\d{16}~\\d{10}$")
        val unitIdPattern = Regex("^ca-app-pub-\\d{16}/\\d{10}$")

        if (rawAppId.isNullOrBlank() || rawAppId == testAppId || !appIdPattern.matches(rawAppId)) {
            throw GradleException(
                "PRODUCTION ADMOB ID VALIDATION FAILED: adMobAppId is missing, set to Google's sample test ID, or invalid. " +
                "Provide a valid production AdMob App ID (ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY) in release-signing.properties or TSKR_ADMOBAPPID."
            )
        }

        if (rawAdUnitId.isNullOrBlank() || rawAdUnitId == testAdUnitId || !unitIdPattern.matches(rawAdUnitId)) {
            throw GradleException(
                "PRODUCTION ADMOB ID VALIDATION FAILED: adMobNativeAdUnitId is missing, set to Google's sample test ID, or invalid. " +
                "Provide a valid production Native Ad Unit ID (ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ) in release-signing.properties or TSKR_ADMOBNATIVEADUNITID."
            )
        }

        logger.lifecycle("SUCCESS: Production AdMob IDs are valid and configured for release.")
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
    implementation("androidx.core:core-ktx:1.12.0")
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
