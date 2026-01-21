// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
    kotlin("plugin.serialization") version "2.3.0" apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    id("com.mikepenz.aboutlibraries.plugin.android") version "14.0.0-b02" apply false
}
