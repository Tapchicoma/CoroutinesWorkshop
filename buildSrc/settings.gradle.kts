pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
}

buildCache {
    local {
        isEnabled = true
    }
}

rootProject.name = "buildSrc"