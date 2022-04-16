import java.text.SimpleDateFormat
import java.util.*

plugins {
    base
    java
    idea // optional (to help generate IntelliJ IDEA project files)
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("com.adarshr.test-logger") version "3.1.0"
}

repositories {
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
}

version = "1.0.0-SNAPSHOT"
extra["isReleaseVersion"] = !version.toString().toLowerCase().endsWith("snapshot")

gradlePlugin {
    plugins {
        create("cassandra") {
            id = "org.carlspring.gradle.plugins.embedded.cassandra"
            group = "org.carlspring.gradle.plugins"
            implementationClass = "org.carlspring.gradle.plugins.embedded.cassandra.EmbeddedCassandraPlugin"
            version = project.version
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(gradleApi())
    implementation("com.github.nosan:embedded-cassandra:4.0.7")
    testImplementation(platform("org.junit:junit-bom:5.8.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite-engine")
}

val testIntegrationSourceSet = sourceSets.create("testIntegration")

// Inherits the dependencies from the master build.gradle.kts
configurations[testIntegrationSourceSet.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
// Append to existing source sets.
gradlePlugin.testSourceSet(testIntegrationSourceSet)

tasks {

    named<Jar>("jar") {
        val attrs = HashMap<String, String?>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz")
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        attrs["Build-Date"] = sdf.format(Date())
        attrs["Build-JDK"] = System.getProperty("java.version")
        attrs["Build-Gradle"] = project.gradle.gradleVersion
        attrs["Build-OS"] = System.getProperty("os.name")
        manifest.attributes(attrs)
    }

    val testTask by named<Test>("test") {
        useJUnitPlatform()
    }

    val testIntegrationTask by register<Test>("testIntegration") {
        group = "verification"

        testClassesDirs = testIntegrationSourceSet.output.classesDirs
        classpath = configurations[testIntegrationSourceSet.runtimeClasspathConfigurationName] +
                testIntegrationSourceSet.output +
                sourceSets.named("main").get().output.classesDirs

        useJUnitPlatform {
            includeEngines("junit-platform-suite-engine", "junit-jupiter")
        }

        testlogger {
            theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
            // set to false to disable detailed failure logs
            showExceptions = true
            // set to false to hide stack traces
            showStackTraces = true
            // set to true to remove any filtering applied to stack traces
            showFullStackTraces = false
            // set to false to hide exception causes
            showCauses = true
            // set threshold in milliseconds to highlight slow tests
            slowThreshold = 20000
            // displays a breakdown of passes, failures and skips along with total duration
            showSummary = true
            // set to true to see simple class names
            showSimpleNames = false
            // set to false to hide passed tests
            showPassed = true
            // set to false to hide skipped tests
            showSkipped = true
            // set to false to hide failed tests
            showFailed = true
            // enable to see standard out and error streams inline with the test results
            showStandardStreams = false
            // set to false to hide passed standard out and error streams
            showPassedStandardStreams = true
            // set to false to hide skipped standard out and error streams
            showSkippedStandardStreams = true
            // set to false to hide failed standard out and error streams
            showFailedStandardStreams = true
        }

        // This task must run after unit tests.
        mustRunAfter(testTask)
    }

    named<Task>("check") {
        dependsOn(testIntegrationTask)
    }

    named<Task>("build") {
        finalizedBy(named("publishToMavenLocal"))
    }

    withType<Sign>() {
        onlyIf {
            (project.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
        }
    }

}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

publishing {
    repositories {
        maven {
            name = "carlspring"
            var repositoryUrl = "https://eu.repo.carlspring.org/content/repositories/carlspring-oss-releases/"
            if (version.toString().endsWith("SNAPSHOT")) {
                repositoryUrl = "https://eu.repo.carlspring.org/content/repositories/carlspring-oss-snapshots/"
            }
            url = uri(repositoryUrl)
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials(org.gradle.api.credentials.PasswordCredentials::class)
        }
    }
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = project.group as String?
            artifactId = project.name
            version = project.version as String?

            withBuildIdentifier()

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name.set("Gradle plugin for starting an embedded cassandra instance during tests.")
                url.set("https://github.com/carlspring/embedded-cassandra-gradle-plugin")
                inceptionYear.set("2022")
                issueManagement {
                    url.set("https://github.com/carlspring/embedded-cassandra-gradle-plugin/issues")
                }
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("carlspring")
                        name.set("Martin Todorov")
                        email.set("carlspring@gmail.com")
                        organization.set("Carlspring Consulting & Development Ltd.")
                        url.set("https://github.com/carlspring")
                    }
                    developer {
                        id.set("steve-todorov")
                        name.set("Steve Todorov")
                        email.set("steve.todorov@carlspring.com")
                        organization.set("Carlspring Consulting & Development Ltd.")
                        url.set("https://github.com/steve-todorov")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/carlspring/embedded-cassandra-gradle-plugin.git")
                    developerConnection.set("scm:git:ssh://github.com/carlspring/embedded-cassandra-gradle-plugin.git")
                    url.set("https://github.com/carlspring/embedded-cassandra-gradle-plugin")
                }
            }
        }
    }
}
