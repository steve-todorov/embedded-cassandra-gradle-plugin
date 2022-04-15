# embedded-cassandra-gradle-plugin

This plugin will create two tasks:

* `startCassandra` - which will start a Cassandra instance
* `stopCassandra` - which will stop the running Cassandra instance

## Configuration

### Kotlin

```kotlin
plugins {
    id("org.carlspring.gradle.plugins.embedded.cassandra") version "1.0.0-SNAPSHOT"
}

cassandra {
    // (Optional) The working directory for cassandra instance - defaults to ${project.buildDir}/cassandra/
    workingDirectory = java.nio.file.Path.of("${project.buildDir}/cassandra/my-example")

    // (Optional) Just a shortcut to apply same settings below as below.
    defaultTestSettings()
  
    // (Optional) Cassandra JVM options
    // (Default)  -Xms1024m, -Xmx1024m
    jvmOptions = listOf("-Xms1024m", "-Xmx1024m");

    // (Optional) Add cassandra.yaml configuration properties
    // (Default)  Has authenticator and authorizer.
    val customConfigProperties = hashMapOf<String, Any?>()
    customConfigProperties["authenticator"] = "PasswordAuthenticator"
    customConfigProperties["authorizer"] = "CassandraAuthorizer"
    customConfigProperties["num_tokens"] = 1 // check notes below for explanation why this is 1 and not 256
    configProperties = customConfigProperties

    // (Optional) Add properties to Cassandra's start up command
    // (Default)  Empty
    // 
    // Settings below are very adequate for TESTING purposes - they disable some replication configuration that
    // results in significantly faster start-up time (before - 50s; after - 12s)
    // 
    // Resources:
    //  https://medium.com/@saidbouras/running-integration-tests-with-apache-cassandra-42305dc260a6
    //  gh/saidbouras/cassandra-docker-unit
    //  gh/asarkar/url-shortener/blob/master/src/test/kotlin/org/asarkar/urlshortener/ApplicationTest.kt#L54
    //  gh/datastax/cassandra-quarkus/pull/7
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

tasks.register<Test>("myCustomIntegrationTest") {
    dependsOn("startCassandra")
    // do some stuff
    finalizedBy("stopCassandra")
}
```

### Gradle

```gradle
plugins {
    id 'java-library'
    id 'org.carlspring.gradle.plugins.embedded.cassandra' version '1.0.0-SNAPSHOT'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter-engine:5.3.1'
}

tasks.register('customTask') {
    dependsOn 'startCassandra'
    finalizedBy 'stopCassandra'
}

tasks.register('customTaskSimulatingFailure') {
    dependsOn 'startCassandra'
    doLast {
        throw RuntimeException('simulating failure')
    }
    finalizedBy 'stopCassandra'
}

cassandra {
    workingDirectory = java.nio.file.Path.of("${project.buildDir}/cassandra/my-example")

    defaultTestSettings()

    jvmOptions = ['-Xms1024m', '-Xmx1024m']

    configProperties = [:]
    configProperties.put('authenticator', 'PasswordAuthenticator')
    configProperties.put('authorizer', 'CassandraAuthorizer')
    configProperties.put('num_tokens', 1)

    systemProperties = [:]
    systemProperties.put('cassandra.skip_wait_for_gossip_to_settle', 0)
    systemProperties.put('cassandra.load_ring_state', false)
    systemProperties.put('cassandra.initial_token', 1)
    systemProperties.put('cassandra.num_tokens', 'nil')
    systemProperties.put('cassandra.allocate_tokens_for_local_replication_factor', 'nil')
    systemProperties.put('num_tokens', 1)
    systemProperties.put('allocate_tokens_for_local_replication_factor', 1)
}

```

## JDK

Supports JDK 11+

## Developer notes

* Test cases are not meant to be executed in parallel - requires too much computing power and would require running 
  Cassandra on different ports which is a hassle to configure.

## Publishing

```
cat >> ~/.gradle/gradle.properties 

carlspringUsername=
carlspringPassword=
signing.gnupg.keyName=
signing.gnupg.useLegacyGpg=true (if binary is gpg; otherwise don't include for gpg2)
```
