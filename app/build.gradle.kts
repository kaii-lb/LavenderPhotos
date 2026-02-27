import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
    id("com.mikepenz.aboutlibraries.plugin.android")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

android {
    namespace = "com.kaii.photos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kaii.photos"
        minSdk = 30
        targetSdk = 36
        versionCode = 140
        versionName = "v1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        splits {
            abi {
                isUniversalApk = false
                isEnable = true

                reset()
                include(includes = arrayOf("armeabi-v7a", "arm64-v8a"))
            }
        }
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

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

baselineProfile {
    dexLayoutOptimization = true

    filter {
        include(pkg = "com.kaii.photos.**")
    }
}

// noinspection UseTomlInstead
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation(platform("androidx.compose:compose-bom:2026.02.00"))
    implementation("androidx.compose.ui:ui:1.10.4")
    implementation("androidx.compose.ui:ui-graphics:1.10.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.4")
    implementation("androidx.compose.material3:material3:1.5.0-alpha15")
    implementation("androidx.compose.animation:animation:1.10.4")
    implementation("androidx.compose.animation:animation-graphics:1.10.4")
    implementation("androidx.compose.runtime:runtime-livedata:1.10.4")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("""androidx.compose.foundation:foundation:1.10.4""")
    implementation("androidx.graphics:graphics-shapes-android:1.1.0")
    implementation("androidx.test:monitor:1.8.0")
    implementation("androidx.test.ext:junit-ktx:1.3.0")
    implementation("androidx.activity:activity-ktx:1.12.4")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-json:2.3.1")
    implementation(libs.androidx.profileinstaller)
    implementation("androidx.work:work-runtime-ktx:2.11.1")
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.rules)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.runtime)

    val media3Version = "1.9.2"
    implementation("androidx.media3:media3-transformer:$media3Version")
    implementation("androidx.media3:media3-effect:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:${roomVersion}")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta08")
    implementation("com.github.bumptech.glide:ktx:1.0.0-beta08")
    implementation("com.github.bumptech.glide:avif-integration:5.0.5")
    implementation("io.github.awxkee:jxl-coder-glide:2.6.0")

    implementation("me.saket.telephoto:zoomable-image-glide:0.18.0")

    implementation("com.github.kaii-lb:Lavender-Snackbars:b79cb851bc")
    implementation("com.kaii.lavender:immich-integration-android:2.0.0") // TODO: un-maven-local this
    // implementation("io.github.kaii-lb.lavender.immichintegration:library-android:2.0.0")

    implementation("com.mikepenz:aboutlibraries:13.2.1")
    implementation("com.mikepenz:aboutlibraries-compose-m3:13.2.1")

    implementation("io.github.pdvrieze.xmlutil:core:0.91.3")
    implementation("io.github.pdvrieze.xmlutil:serialization:0.91.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0-beta01")

    debugImplementation("androidx.compose.ui:ui-tooling:1.10.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.4")

    ksp("com.github.bumptech.glide:ksp:5.0.5")
    ksp("androidx.room:room-compiler:$roomVersion")

    baselineProfile(project(":baselineprofile"))
}

