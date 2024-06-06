pluginManagement {
    repositories {
        google()
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
            name = "webrtc"
            url = uri("https://maven.pkg.github.com/cloudmix/webrtc-android-releases")
            credentials {
                username = "my_github_username"
                password = "ghp_xxxxxxxxxxxxxxxxxxxx"
            }
        }
    }
}

rootProject.name = "WHEP example"
include(":app")
