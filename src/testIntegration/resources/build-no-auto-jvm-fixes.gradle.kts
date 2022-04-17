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
    testImplementation(kotlin("stdlib", "1.3.21"))
    testImplementation(kotlin("test-junit5", "1.3.21"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}

tasks.register<Task>("customTask") {
    dependsOn("startCassandra")
    doLast {
        println("cassandra.storage.port = " + System.getProperty("cassandra.storage.port"));
        println("cassandra.storage.port.ssl = " + System.getProperty("cassandra.storage.port.ssl"));
        println("cassandra.native.transport.port = " + System.getProperty("cassandra.native.transport.port"));
    }
    finalizedBy("stopCassandra")
}

cassandra {
    cassandra4JVMAutoFix = false
    workingDirectory = Path.of("${project.buildDir}/cassandra/my-example")
    defaultTestSettings()
}
