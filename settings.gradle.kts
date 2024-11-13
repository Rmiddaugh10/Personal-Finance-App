@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://jitpack.io")
            // No credentials here
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            credentials {
                // Instead of using findProperty, you can reference extra properties
                username = extra["gpr.user"] as String
                password = extra["gpr.token"] as String
            }
        }
    }
}

// Root project configuration
rootProject.name = "MyApplication3"
include(":app")

