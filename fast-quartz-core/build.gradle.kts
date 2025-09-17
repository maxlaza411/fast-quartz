plugins {
    `java-library`
}

base {
    archivesName.set("fast-quartz-core")
}

dependencies {
    api(project(":fabric-stubs"))
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("org.slf4j:slf4j-api:2.0.13")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
