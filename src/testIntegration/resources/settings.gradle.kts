rootProject.name = "integration-test-kotlin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        flatDir {
            dirs("libs")
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.carlspring.gradle.plugins.embedded.cassandra") {
                useModule("org.carlspring.gradle.plugins:embedded-cassandra-gradle-plugin:${requested.version}")
            }
        }
    }
}
