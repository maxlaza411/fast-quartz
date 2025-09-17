plugins {
    application
}

base {
    archivesName.set("headless-runner")
}

dependencies {
    implementation(project(":fabric-stubs"))
    implementation(project(":fast-quartz-core"))
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

application {
    mainClass.set("dev.fastquartz.headless.HeadlessServerLauncher")
}
