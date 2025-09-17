pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
    plugins {
        id("com.diffplug.spotless") version "6.25.0"
        id("net.ltgt.errorprone") version "3.1.0"
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://libraries.minecraft.net")
    }
}

rootProject.name = "fast-quartz"
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
include("fabric-stubs")
include("fast-quartz-core")
include("headless-runner")
