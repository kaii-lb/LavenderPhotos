import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.mikepenz.aboutlibraries.android)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

android {
    namespace = "com.kaii.photos"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kaii.photos"
        minSdk = 30
        targetSdk = 37
        versionCode = 200
        versionName = "v2.0.0"

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

        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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

    androidResources {
        generateLocaleConfig = true

        localeFilters.addAll(
            listOf(
                "en", "ar", "ca", "cs", "da",
                "de", "el", "es", "et", "fr",
                "gl", "hy", "in", "it", "ja",
                "kn", "pl", "pt", "pt-rBR", "ru",
                "sv", "tr", "uk", "vl", "zh",
                "zh-rCN", "zh-rTW"
            )
        )
    }
}

baselineProfile {
    dexLayoutOptimization = true

    filter {
        include(pkg = "com.kaii.photos.**")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.google.material)
    implementation(libs.google.zxing)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)

    implementation(libs.androidx.graphics.shapes.android)

    implementation(libs.androidx.junit.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.monitor)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.test.rules)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.glide)
    implementation(libs.glide.avif.integration)
    implementation(libs.glide.compose)
    implementation(libs.glide.jxl.coder)
    implementation(libs.glide.ktx)

    implementation(libs.kittinunf.fuel)
    implementation(libs.kittinunf.fuel.json)

    implementation(libs.lavender.immichintegration.android)
    implementation(libs.lavender.snackbars.android)

    implementation(libs.mikepenz.aboutlibraries)
    implementation(libs.mikepenz.aboutlibraries.compose)

    implementation(libs.pdvrieze.xmlutil.core)
    implementation(libs.pdvrieze.xmlutil.serialization)

    implementation(libs.saket.zoomable.image.glide)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.uiautomator)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    ksp(libs.glide.ksp)
    ksp(libs.androidx.room.compiler)

    baselineProfile(project(":baselineprofile"))
}

