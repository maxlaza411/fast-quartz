plugins {
    id("fabric-loom")
    application
}

base {
    archivesName.set("headless-runner")
}

dependencies {
    minecraft("com.mojang:minecraft:1.20.1")
    mappings("net.fabricmc:yarn:1.20.1+build.10:v2")
    modImplementation("net.fabricmc:fabric-loader:0.15.11")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.91.0+1.20.1")

    modImplementation(project(":fast-quartz-core"))
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

application {
    mainClass.set("dev.fastquartz.headless.HeadlessServerLauncher")
}

loom {
    runs {
        configureEach {
            ideConfigGenerated(false)
        }
    }
}
