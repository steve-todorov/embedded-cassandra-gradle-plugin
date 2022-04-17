package org.carlspring.gradle.plugins.embedded.cassandra;

import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public abstract class EmbeddedCassandraExtension
{

    private final String name;

    private final Project project;

    private Path workingDirectory;

    private HashMap<String, Object> configProperties = new HashMap<>();

    private HashMap<String, Object> systemProperties = new HashMap<>();

    private List<String> jvmOptions = new ArrayList<>();

    private Duration startupTimeout = Duration.ofMillis(120000L);

    // Apply JVM option fixes to Cassandra when running on JDK >= 15
    private boolean cassandra4JVMAutoFix = true;

    @Inject
    public EmbeddedCassandraExtension(String name, Project project)
    {
        this.name = name;
        this.project = project;

        // Default config properties.
        configProperties.put("authenticator", "PasswordAuthenticator");
        configProperties.put("authorizer", "CassandraAuthorizer");

        // Default system properties.
        systemProperties.put("cassandra.superuser_setup_delay_ms", 0);

        // Add default jvm options
        jvmOptions.add("-Xms1024m");
        jvmOptions.add("-Xmx1024m");
    }

    public String getName()
    {
        return this.name;
    }

    public Path getWorkingDirectory()
    {
        if (workingDirectory == null) {
            workingDirectory = project.getBuildDir().toPath().resolve("cassandra");
        }

        return this.workingDirectory;
    }

    @Input
    public void setWorkingDirectory(Path path)
    {
        this.workingDirectory = path;
    }

    @Input
    public void setWorkingDirectory(String path)
    {
        this.workingDirectory = Path.of(path);
    }

    public HashMap<String, Object> getConfigProperties()
    {
        return configProperties;
    }

    @Input
    @Optional
    public void setConfigProperties(HashMap<String, Object> configProperties)
    {
        this.configProperties = configProperties;
    }

    public HashMap<String, Object> getSystemProperties()
    {
        return systemProperties;
    }

    @Input
    @Optional
    public void setSystemProperties(HashMap<String, Object> systemProperties)
    {
        this.systemProperties = systemProperties;
    }

    public List<String> getJvmOptions()
    {
        return jvmOptions;
    }

    @Input
    @Optional
    public void setJvmOptions(List<String> jvmOptions)
    {
        this.jvmOptions = jvmOptions;
    }

    public boolean getCassandra4JVMAutoFix()
    {
        return cassandra4JVMAutoFix;
    }

    @Input
    @Optional
    public void setCassandra4JVMAutoFix(boolean cassandra4JVMAutoFix)
    {
        this.cassandra4JVMAutoFix = cassandra4JVMAutoFix;
    }

    public Duration getStartupTimeout()
    {
        return startupTimeout;
    }

    @Input
    @Optional
    public void setStartupTimeout(Duration startupTimeout)
    {
        this.startupTimeout = startupTimeout;
    }

    public void defaultTestSettings()
    {
        defaultTestSettings(false);
    }

    public void defaultTestSettings(boolean randomPorts)
    {
        jvmOptions.addAll(List.of("-Xms2048m", "-Xmx2048m"));

        configProperties.put("authenticator", "PasswordAuthenticator");
        configProperties.put("authorizer", "CassandraAuthorizer");
        configProperties.put("num_tokens", 1);

        // Setting random cassandra ports to enable parallel tests.
        Integer storagePort = randomPorts ? findRandomOpenPort() : 7000;
        Integer storagePortSsl = randomPorts ? findRandomOpenPort() : 7001;
        Integer nativeTransportPort = randomPorts ? findRandomOpenPort() : 9042;
        configProperties.put("storage_port", storagePort); // default is 7000
        configProperties.put("ssl_storage_port", storagePortSsl); // default is 7001
        configProperties.put("native_transport_port", nativeTransportPort); // default is 9042
        // /Setting random cassandra ports to enable parallel tests.

        configProperties.put("hints_flush_period_in_ms", 5000); // defaults to 10000
        configProperties.put("commitlog_sync_period_in_ms", 5000); // defaults to 10000
        configProperties.put("range_request_timeout_in_ms", 5000); // defaults to 10000
        configProperties.put("request_timeout_in_ms", 5000); // defaults to 10000

        // Settings below are very adequate for TESTING purposes - they disable some replication configuration that
        // results in significantly faster start-up time (before - 50s; after - 12s)
        //
        // Resources:
        //  https://medium.com/@saidbouras/running-integration-tests-with-apache-cassandra-42305dc260a6
        //  gh/saidbouras/cassandra-docker-unit
        //  gh/asarkar/url-shortener/blob/master/src/test/kotlin/org/asarkar/urlshortener/ApplicationTest.kt#L54
        //  gh/datastax/cassandra-quarkus/pull/7
        systemProperties.put("cassandra.skip_wait_for_gossip_to_settle", 0);
        systemProperties.put("cassandra.load_ring_state", false);
        systemProperties.put("cassandra.initial_token", 1);
        systemProperties.put("cassandra.num_tokens", "nil");
        systemProperties.put("cassandra.allocate_tokens_for_local_replication_factor", "nil");
        systemProperties.put("num_tokens", 1);
        systemProperties.put("allocate_tokens_for_local_replication_factor", 1);

        // Cassandra takes longer on machines with less than 4 CPUS (i.e. Github Actions)
        startupTimeout = Duration.ofMillis(240000L);
    }

    public synchronized Integer findRandomOpenPort()
    {
        try (java.net.ServerSocket socket = new ServerSocket(new SecureRandom().nextInt(65000 - 35000 + 1) + 35000)) {
            return socket.getLocalPort();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
