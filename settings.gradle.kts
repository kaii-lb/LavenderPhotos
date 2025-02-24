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
        // maven {
        // 	url = uri("https://jitpack.io")
        // }
    }
}

rootProject.name = "Photos"
include(":app")
include(":lavender_snackbars")

includeBuild("../LavenderSnackbars") {
	dependencySubstitution {
		substitute(module("com.kaii.lavender_snackbars:Lavender-Snackbars")).using(project(":lavender_snackbars"))
	}
}
