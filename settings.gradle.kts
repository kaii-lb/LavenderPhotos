pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "Photos"
include(":app")
include(":baselineprofile")

//includeBuild("../telephoto") {
//  dependencySubstitution {
//    substitute(module("me.saket.telephoto:zoomable")).using(project(":zoomable"))
//    substitute(module("me.saket.telephoto:zoomable-image")).using(project(":zoomable-image:core"))
//    substitute(module("me.saket.telephoto:zoomable-image-glide")).using(project(":zoomable-image:glide"))
//  }
//}
