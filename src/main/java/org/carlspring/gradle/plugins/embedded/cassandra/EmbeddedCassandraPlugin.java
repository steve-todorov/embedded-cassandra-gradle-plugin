package org.carlspring.gradle.plugins.embedded.cassandra;

import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.CassandraBuilder;
import com.github.nosan.embedded.cassandra.SimpleSeedProviderConfigurator;
import com.github.nosan.embedded.cassandra.commons.ClassPathResource;
import org.carlspring.gradle.plugins.embedded.cassandra.tasks.StartCassandraTask;
import org.carlspring.gradle.plugins.embedded.cassandra.tasks.StopCassandraTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class EmbeddedCassandraPlugin
        implements Plugin<Project>
{

    public static final String JVM_AUTOFIX_MESSAGE = "[WARNING] Changes to cassandra/conf/jvm11-clients.options and cassandra/conf/jvm11-server.options have been " +
                                                     "automatically applied to allow Cassandra to start.";

    public final String EXTENSION_NAME = "cassandra";

    private volatile Cassandra instance;

    @Override
    public void apply(final Project project)
    {

        EmbeddedCassandraExtension container = project.getExtensions()
                                                      .create(EXTENSION_NAME, EmbeddedCassandraExtension.class, EXTENSION_NAME, project);

        project.afterEvaluate(p -> {
            CassandraBuilder builder = new CassandraBuilder();
            builder.workingDirectory(container::getWorkingDirectory);
            builder.registerShutdownHook(true);

            // Named as jvm15 but copied as jvm11 on purpose, because Cassandra doesn't know anything about JDK >= 11 yet so naming it differently
            // will not work.
            int jdkVersion = Runtime.version().feature();
            if (jdkVersion >= 15 && jdkVersion < 18) {
                if (builder.getVersion().getMajor() <= 4 && container.getCassandra4JVMAutoFix()) {
                    builder.addWorkingDirectoryResource(new ClassPathResource("cassandra/server/jvm17-clients.options"), "conf/jvm11-clients.options")
                           .addWorkingDirectoryResource(new ClassPathResource("cassandra/server/jvm17-server.options"), "conf/jvm11-server.options");
                }
            }
            else if (jdkVersion == 18) {
                String msg = "Cassandra needs SecurityManager which has been removed in JDK >= 18 -- your version is " + Runtime.version().toString();
                throw new RuntimeException(msg);
            }

            builder.jvmOptions(container.getJvmOptions());
            builder.configProperties(container.getConfigProperties());
            builder.systemProperties(container.getSystemProperties());

            builder.configure(new SimpleSeedProviderConfigurator("127.0.0.1:" + container.getConfigProperties()
                                                                                         .getOrDefault("storage_port", 7000)));

            Integer storagePort = (Integer) container.getConfigProperties().getOrDefault("storage_port", 7000);
            Integer storagePortSsl = (Integer) container.getConfigProperties().getOrDefault("ssl_storage_port", 7001);
            Integer nativeTransportPort = (Integer) container.getConfigProperties().getOrDefault("native_transport_port", 9042);

            System.getProperties().put("cassandra.storage.port", String.valueOf(storagePort));
            System.getProperties().put("cassandra.storage.port.ssl", String.valueOf(storagePortSsl));
            System.getProperties().put("cassandra.native.transport.port", String.valueOf(nativeTransportPort));

            builder.startupTimeout(container.getStartupTimeout());

            if (instance == null) {
                instance = builder.build();
            }
        });

        project.getTasks()
               .register("startCassandra", StartCassandraTask.class, task -> {
                   task.setGroup("cassandra");
                   task.getInstance().set(instance);
               });

        project.getTasks()
               .register("stopCassandra", StopCassandraTask.class, task -> {
                   task.setGroup("cassandra");
                   task.getInstance().set(instance);
               });
    }

    public Cassandra getInstance()
    {
        return instance;
    }

}
