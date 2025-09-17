plugins {
    `java-library`
}

base {
    archivesName.set("faststone-core")
}

dependencies {
    api(project(":fabric-stubs"))
    implementation("org.slf4j:slf4j-api:2.0.13")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
