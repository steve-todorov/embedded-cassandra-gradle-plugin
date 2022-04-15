package org.carlspring.gradle.plugins.embedded.cassandra.tests;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationTest
{

    // NOTE: This test will always take longer, because it uses production ready configuration for bootstrapping Cassandra.
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "build-no-config.gradle,settings.gradle",
            "build-no-config.gradle.kts,settings.gradle.kts",
    })
    public void shouldStartAndStopCassandraWithoutConfiguration(String buildFile, String settingsFile) throws IOException
    {
        Path projectDir = generateProject(settingsFile, buildFile);

        // Run the build
        BuildResult result = GradleRunner.create().forwardOutput().withPluginClasspath().withDebug(true)
                                         .withArguments("customTask", "--stacktrace")
                                         .withProjectDir(projectDir.toFile())
                                         .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":startCassandra").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":customTask").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":stopCassandra").getOutcome());
        assertTrue(result.getOutput().contains("Starting Cassandra in"));
        assertTrue(result.getOutput().contains("Stopping Cassandra in"));
        assertTrue(result.getOutput().contains(projectDir.resolve("build/cassandra").toString()));

    }

    @EnabledForJreRange(max = JRE.JAVA_14)
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "build-no-auto-jvm-fixes.gradle,settings.gradle",
            "build-no-auto-jvm-fixes.gradle.kts,settings.gradle.kts",
    })
    public void shouldSucceedWhenNoJVMFixesAndUsingSupportedJDK(String buildFile, String settingsFile) throws IOException
    {
        Path projectDir = generateProject(settingsFile, buildFile);
        // Paths.get("build/generated/testIntegrationProjects/" + type + "/shouldSucceedWhenNoJVMFixesAndUsingSupportedJDK");

        // Run the build
        BuildResult result = GradleRunner.create().forwardOutput().withPluginClasspath().withDebug(true)
                                         .withArguments("customTask", "--stacktrace")
                                         .withProjectDir(projectDir.toFile())
                                         .build();

        // When running on JDK <= 15 the build should Succeed.
        assertEquals(TaskOutcome.SUCCESS, result.task(":startCassandra").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":customTask").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":stopCassandra").getOutcome());
        assertTrue(result.getOutput().contains("Starting Cassandra in"));
        assertTrue(result.getOutput().contains("Stopping Cassandra in"));
        assertTrue(result.getOutput().contains(projectDir.resolve("build/cassandra/my-example").toString()));
    }

    @EnabledForJreRange(min = JRE.JAVA_15)
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "build-no-auto-jvm-fixes.gradle,settings.gradle",
            "build-no-auto-jvm-fixes.gradle.kts,settings.gradle.kts",
    })
    public void shouldFailWhenNoJVMFixesAndUnsupportedJDK(String buildFile, String settingsFile) throws IOException
    {
        Path projectDir = generateProject(settingsFile, buildFile);

        // Run the build
        BuildResult result = GradleRunner.create().forwardOutput().withPluginClasspath().withDebug(true)
                                         .withArguments("customTask", "--stacktrace")
                                         .withProjectDir(projectDir.toFile())
                                         .buildAndFail();

        // When running on JDK 15+ the build should FAIL.
        assertEquals(TaskOutcome.FAILED, result.task(":startCassandra").getOutcome());
        assertTrue(result.getOutput().contains("Starting Cassandra in"));
        assertFalse(result.getOutput().contains("Stopping Cassandra in"));
        assertTrue(result.getOutput().contains("Error: A fatal exception has occurred. Program will exit"));
        assertTrue(result.getOutput().contains(projectDir.resolve("build/cassandra/my-example").toString()));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "build-with-config.gradle,settings.gradle",
            "build-with-config.gradle.kts,settings.gradle.kts",
            "build-with-defaultTestSettings.gradle,settings.gradle",
            "build-with-defaultTestSettings.gradle.kts,settings.gradle.kts",
    })
    public void shouldStartAndStopCassandra(String buildFile, String settingsFile) throws IOException
    {
        Path projectDir = generateProject(settingsFile, buildFile);

        // Run the build
        BuildResult result = GradleRunner.create().forwardOutput().withPluginClasspath().withDebug(true)
                                         .withArguments("customTask", "--stacktrace")
                                         .withProjectDir(projectDir.toFile())
                                         .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":startCassandra").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":customTask").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":stopCassandra").getOutcome());

        assertTrue(result.getOutput().contains("Starting Cassandra in"));
        assertTrue(result.getOutput().contains("Stopping Cassandra in"));
        assertTrue(result.getOutput().contains(projectDir.resolve("build/cassandra/my-example").toString()));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "build-with-config.gradle,settings.gradle",
            "build-with-config.gradle.kts,settings.gradle.kts",
    })
    public void shouldStopCassandraOnFailure(String buildFile, String settingsFile) throws IOException
    {
        Path projectDir = generateProject(settingsFile, buildFile);

        // Run the build
        BuildResult result = GradleRunner.create().forwardOutput().withPluginClasspath().withDebug(true)
                                         .withArguments("customTaskSimulatingFailure", "--stacktrace")
                                         .withProjectDir(projectDir.toFile())
                                         .buildAndFail();

        assertEquals(TaskOutcome.SUCCESS, result.task(":startCassandra").getOutcome());
        assertEquals(TaskOutcome.FAILED, result.task(":customTaskSimulatingFailure").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":stopCassandra").getOutcome());
        assertTrue(result.getOutput().contains("Starting Cassandra in"));
        assertTrue(result.getOutput().contains("Stopping Cassandra in"));
        assertTrue(result.getOutput().contains(projectDir.resolve("build/cassandra/my-example").toString()));
        assertTrue(result.getOutput().contains("customTaskSimulatingFailure FAILED"));
    }

    private Path generateProject(String settingsFile, String buildFile) throws IOException
    {
        // Setup the test build directory
        Path projectDir = Paths.get("build/generated/testIntegrationProjects");
        projectDir = projectDir.resolve(buildFile.endsWith(".gradle.kts") ? "kotlin" : "groovy");

        // Requires JDK 9 ++
        StackWalker.StackFrame frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                                                  .walk(stream1 -> stream1.skip(1).findFirst().orElse(null));

        if (frame != null) {
            projectDir = projectDir.resolve(frame.getMethodName());
        }
        else {
            projectDir = projectDir.resolve(UUID.randomUUID().toString().substring(24, 30));
        }

        if (projectDir.toAbsolutePath().toFile().exists()) {
            System.out.println("Cleaning " + projectDir);
            Files.walk(projectDir.toAbsolutePath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
        Files.createDirectories(projectDir);

        InputStream settingsFileIs = IntegrationTest.class.getClassLoader().getResourceAsStream(settingsFile);
        writeFile(projectDir.resolve(settingsFile), settingsFileIs);

        InputStream buildFileIs = IntegrationTest.class.getClassLoader().getResourceAsStream(buildFile);
        Path buildFilePath = projectDir.resolve("build.gradle");
        if (buildFile.endsWith(".gradle.kts")) {
            buildFilePath = projectDir.resolve("build.gradle.kts");
        }
        writeFile(buildFilePath, buildFileIs);

        return projectDir;
    }

    private void writeFile(Path file, InputStream buffer) throws IOException
    {
        Files.copy(buffer, file, StandardCopyOption.REPLACE_EXISTING);
        buffer.close();
    }


}
