import java.text.SimpleDateFormat
import java.util.*

plugins {
    base
    java
    idea // optional (to help generate IntelliJ IDEA project files)
    `java-gradle-plugin`
    `maven-publish`
    signing
    alias(pluginLibs.plugins.test.logger)
}

repositories {
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
}

extra["isReleaseVersion"] = !version.toString().toLowerCase().endsWith("snapshot")

println("Building version: $version")

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
    implementation(appLibs.embedded.cassandra)
    testImplementation(platform(testLibs.junit.bom))
    testImplementation(testLibs.junit.jupiter)
    testImplementation(testLibs.junit.platform.suite.engine)
}

// Configure multiple test sources
testing {
    suites {
        // Just for self reference, technically this is already configured by default.
        val test by getting(JvmTestSuite::class) {
            targets {
                all {
                    testTask.configure {
                        useJUnitPlatform {
                            filter {
                                setFailOnNoMatchingTests(false)
                            }
                        }
                    }
                }
            }
        }

        // testIntegration test sources
        //check https://github.com/gradle/gradle/pull/21012 for more examples
        val testIntegration by registering(JvmTestSuite::class) {
            val self = this
            testType.set(TestSuiteType.INTEGRATION_TEST)

            // We need to manually add the "main" sources to the classpath.
            val testIntegrationSourceSet by sourceSets.named(self.name) {
                compileClasspath += sourceSets.main.get().output
                runtimeClasspath += sourceSets.main.get().output
            }

            // Inherit implementation, runtime and test dependencies (adds them to the compile classpath)
            configurations.named("${self.name}Implementation") {
                extendsFrom(configurations.testImplementation.get())
                extendsFrom(configurations.runtimeOnly.get())
                extendsFrom(configurations.implementation.get())
            }

            // Append the testIntegration source set to gradle plugin metadata.
            gradlePlugin.testSourceSet(testIntegrationSourceSet)

            // suite targets = collection of tests to be executed in a particular context (i.e. OS, JDK, etc)
            targets {
                all {
                    // reminder: testTask references the "testIntegration" task - not to the "test" one.
                    testTask.configure {
                        useJUnitPlatform {
                            includeEngines("junit-jupiter")

                            // maxParallelForks will use multiple VMs and JUnit will parallelize test execution according to the configured
                            // parallelism in each of them.
                            // For example with maxParallelForks = 2 and junit.jupiter.execution.parallel.config.fixed.parallelism = 4,
                            // Gradle will fork two VMs and distribute the found test classes evenly among them. In each VM, JUnit will
                            // execute the tests in 4 concurrent threads.
                            systemProperty("junit.jupiter.execution.parallel.enabled", false)
                            systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
                            systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                            systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
                            systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", 1)
                        }
                        shouldRunAfter(test)
                    }
                }
            }

            // Make sure the integration test is executed as part of the "check" task.
            tasks.named<Task>("check") {
                dependsOn(named<JvmTestSuite>(self.name))
            }

            // logging config.
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
                slowThreshold = 25000
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
        }
    }
}

tasks {

    named<Jar>("jar") {
        val attrs = HashMap<String, String?>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz")
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        attrs["Build-Date"] = sdf.format(Date())
        attrs["Build-JDK"] = System.getProperty("java.version")
        attrs["Build-Gradle"] = project.gradle.gradleVersion
        attrs["Build-OS"] = System.getProperty("os.name")
        attrs["Build-Automatic"] = System.getProperty("CI", "false")
        manifest.attributes(attrs)
    }

    named<Task>("build") {
        finalizedBy(named("publishToMavenLocal"))
    }

    withType<Sign>() {
        onlyIf {
            (project.extra["isReleaseVersion"] as Boolean) && gradle.taskGraph.hasTask("publish")
        }
    }

    create<Task>("printSourceSetInformation") {
        group = "help"
        doLast {
            sourceSets.forEach { srcSet ->
                println("=================================")
                println("|| SourceSet: " + srcSet.name)
                println("=================================")
                println("|| + Source directories: " + srcSet.allJava.srcDirs)
                println("|| + Output directories: " + srcSet.output.classesDirs.files)
                println("|| + Resource directories: " + srcSet.resources.srcDirs)
                println("|| + Compile classpath:")
                srcSet.compileClasspath.files.sorted().forEach {
                    println("|| ++> " + it.path)
                }
                println("=================================")
                println()
            }
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
