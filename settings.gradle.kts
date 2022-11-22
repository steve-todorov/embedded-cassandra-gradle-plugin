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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Aliases with "-" in the name are referenced by replacing the dash with a dot: "reactor-core" => "reactor.core"
dependencyResolutionManagement {
    versionCatalogs {
        create("appLibs") {
            version("embedded-cassandra", "4.1.1")
            library("embedded-cassandra", "com.github.nosan", "embedded-cassandra-spring-boot-starter").versionRef("embedded-cassandra")
        }
        create("testLibs") {
            version("junit", "5.8.2")
            version("junit-engine", "1.8.2")
            library("junit-bom", "org.junit", "junit-bom").versionRef("junit")
            library("junit-jupiter", "org.junit.jupiter", "junit-jupiter").versionRef("junit")
            library("junit-jupiter-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
            library("junit-platform-suite-engine", "org.junit.platform", "junit-platform-suite-engine").versionRef("junit-engine")
            library("junit-platform-launcher", "org.junit.platform", "junit-platform-launcher").versionRef("junit-engine")
        }
        create("pluginLibs") {
            // Add gradle plugin versions here as we go.
            // Usage -> plugins { alias(pluginDeps.plugins.myDefinedPluginAliasName ) }
            // For more details check https://docs.gradle.org/current/userguide/platforms.html#sec:plugins

            // Gradle Test Logger Plugin
            // https://github.com/radarsh/gradle-test-logger-plugin
            // https://plugins.gradle.org/plugin/com.adarshr.test-logger
            plugin("test-logger", "com.adarshr.test-logger").version("3.1.0")

            // Lombok Gradle Plugin
            // https://docs.freefair.io/gradle-plugins/6.5.0.2/reference/
            // https://plugins.gradle.org/plugin/io.freefair.lombok
            plugin("lombok", "io.freefair.lombok").version("6.5.0.3")
        }
    }
}
