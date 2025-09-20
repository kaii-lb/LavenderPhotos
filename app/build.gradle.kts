import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
    id("com.mikepenz.aboutlibraries.plugin.android")
}

android {
    namespace = "com.kaii.photos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kaii.photos"
        minSdk = 30
        targetSdk = 36
        versionCode = 100
        versionName = "v1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin.compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    baselineProfile {
        dexLayoutOptimization = true
    }
}

// noinspection UseTomlInstead
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2025.09.00"))
    implementation("androidx.compose.ui:ui:1.9.1")
    implementation("androidx.compose.ui:ui-graphics:1.9.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.1")
    implementation("androidx.compose.material3:material3:1.5.0-alpha04")
    implementation("androidx.compose.animation:animation:1.9.1")
    implementation("androidx.compose.animation:animation-graphics:1.9.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("""androidx.compose.foundation:foundation:1.9.1""")
    implementation("androidx.graphics:graphics-shapes-android:1.0.1")
    implementation("androidx.test:monitor:1.8.0")
    implementation("androidx.test.ext:junit-ktx:1.3.0")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-json:2.3.1")
    implementation(libs.androidx.profileinstaller)
    implementation("androidx.work:work-runtime-ktx:2.10.4")
    implementation(libs.androidx.documentfile)
    "baselineProfile"(project(":baselineprofile"))

    val media3Version = "1.8.0"
    implementation("androidx.media3:media3-transformer:$media3Version")
    implementation("androidx.media3:media3-effect:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    val roomVersion = "2.8.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta08")
    implementation("com.github.bumptech.glide:ktx:1.0.0-beta08")
    implementation("com.github.bumptech.glide:avif-integration:5.0.5")

    implementation("com.github.kaii-lb:Lavender-Snackbars:b79cb851bc")
    implementation("com.github.kaii-lb.LavenderImmichIntegration:library:v1.0.9")

    implementation("com.mikepenz:aboutlibraries:13.0.0-rc01")
    implementation("com.mikepenz:aboutlibraries-compose-m3:13.0.0-rc01")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0-alpha06")

    debugImplementation("androidx.compose.ui:ui-tooling:1.9.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.1")

    ksp("com.github.bumptech.glide:ksp:5.0.5")
    ksp("androidx.room:room-compiler:$roomVersion")
}

