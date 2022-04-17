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
    doLast {
        println("cassandra.storage.port = " + System.getProperty("cassandra.storage.port"));
        println("cassandra.storage.port.ssl = " + System.getProperty("cassandra.storage.port.ssl"));
        println("cassandra.native.transport.port = " + System.getProperty("cassandra.native.transport.port"));
    }
    finalizedBy("stopCassandra")
}

cassandra {
    workingDirectory = Path.of("${project.buildDir}/cassandra/my-example")
    defaultTestSettings()
    configProperties.put("storage_port", 18000) // default is 7000
    configProperties.put("ssl_storage_port", 18001) // default is 7001
    configProperties.put("native_transport_port", 18002) // default is 9042
}
