package org.carlspring.gradle.plugins.embedded.cassandra;

import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.CassandraBuilder;
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

                if (builder.getVersion().getMajor() <= 4) {
                    System.out.println("[WARNING] You have requested Cassandra v4 which officially supports only for JDK 8 and 11. However you are " +
                                       "about to run this build using " + Runtime.version().toString() + " which is not officially supported yet.");

                    if(container.getCassandra4JVMAutoFix()) {
                        System.out.println(JVM_AUTOFIX_MESSAGE);
                        System.out.println("[WARNING] To disable this set cassandra { cassandra4JDKAutoFix = false } in the DSL\n");

                        builder.addWorkingDirectoryResource(new ClassPathResource("cassandra/server/jvm17-clients.options"), "conf/jvm11-clients.options")
                               .addWorkingDirectoryResource(new ClassPathResource("cassandra/server/jvm17-server.options"), "conf/jvm11-server.options");
                    }

                }
            }
            else if (jdkVersion == 18) {
                String msg = "Cassandra needs SecurityManager which has been removed in JDK >= 18 -- your version is " + Runtime.version().toString();
                throw new RuntimeException(msg);
            }

            builder.jvmOptions(container.getJvmOptions());
            builder.configProperties(container.getConfigProperties());
            builder.systemProperties(container.getSystemProperties());

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
