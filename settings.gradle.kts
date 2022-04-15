rootProject.name = "embedded-cassandra-gradle-plugin"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.carlspring.gradle.plugins.embedded.cassandra") {
                useModule("org.carlspring.gradle.plugins:embedded-cassandra-gradle-plugin:${requested.version}")
            }
        }
    }
}
