import java.nio.file.Path

plugins {
    kotlin("jvm") version "1.3.21"
    id("org.carlspring.gradle.plugins.embedded.cassandra") version "1.0.0-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}

tasks.register<Task>("customTask") {
    dependsOn("startCassandra")
    finalizedBy("stopCassandra")
}

tasks.register<Task>("customTaskSimulatingFailure") {
    dependsOn("startCassandra")
    doLast {
        throw RuntimeException("simulating failure")
    }
    finalizedBy("stopCassandra")
}

cassandra {
    workingDirectory = Path.of("${project.buildDir}/cassandra/my-example")
    defaultTestSettings()
    // append more settings.
}
