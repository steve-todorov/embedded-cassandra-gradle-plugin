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

tasks.register<Task>("customTaskSimulatingFailure") {
    dependsOn("startCassandra")
    doLast {
        println("cassandra.storage.port = " + System.getProperty("cassandra.storage.port"));
        println("cassandra.storage.port.ssl = " + System.getProperty("cassandra.storage.port.ssl"));
        println("cassandra.native.transport.port = " + System.getProperty("cassandra.native.transport.port"));
        throw RuntimeException("simulating failure")
    }
    finalizedBy("stopCassandra")
}

cassandra {
    workingDirectory = Path.of("${project.buildDir}/cassandra/my-example")

    jvmOptions = listOf("-Xms2048m", "-Xmx2048m");

    val customConfigProperties = hashMapOf<String, Any?>()
    customConfigProperties["authenticator"] = "PasswordAuthenticator"
    customConfigProperties["authorizer"] = "CassandraAuthorizer"
    customConfigProperties["num_tokens"] = 1
    configProperties = customConfigProperties

    val customSystemProperties = hashMapOf<String, Any?>()
    customSystemProperties["cassandra.skip_wait_for_gossip_to_settle"] = 0
    customSystemProperties["cassandra.load_ring_state"] = false
    customSystemProperties["cassandra.initial_token"] = 1
    customSystemProperties["cassandra.num_tokens"] = "nil"
    customSystemProperties["cassandra.allocate_tokens_for_local_replication_factor"] = "nil"
    customSystemProperties["num_tokens"] = 1
    customSystemProperties["allocate_tokens_for_local_replication_factor"] = 1
    systemProperties = customSystemProperties

}
