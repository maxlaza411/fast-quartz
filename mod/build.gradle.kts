plugins {
    `java-library`
}

description = "Fast Quartz Fabric integration layer"

java {
    withSourcesJar()
}

dependencies {
    api(project(":engine"))
    implementation("org.slf4j:slf4j-api:2.0.13")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
