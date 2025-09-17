import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

plugins {
    id("com.diffplug.spotless") apply false
    id("net.ltgt.errorprone") apply false
}

allprojects {
    group = "dev.fastquartz"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "net.ltgt.errorprone")

    configurations.configureEach {
        if (!name.startsWith("spotless")) {
            resolutionStrategy {
                activateDependencyLocking()
            }
        }
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release.set(17)
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Werror"))
            options.isDeprecation = true
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension>("spotless") {
        java {
            target("src/**/*.java")
            googleJavaFormat("1.17.0").reflowLongStrings()
            endWithNewline()
            trimTrailingWhitespace()
        }
    }

    plugins.withId("java") {
        dependencies {
            add("errorprone", "com.google.errorprone:error_prone_core:2.27.1")
            add("testImplementation", platform("org.junit:junit-bom:5.10.2"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
        }

        tasks.named("check") {
            dependsOn("spotlessCheck")
        }
    }
}
