package org.carlspring.gradle.plugins.embedded.cassandra.tasks;

import com.github.nosan.embedded.cassandra.Cassandra;
import org.carlspring.gradle.plugins.embedded.cassandra.EmbeddedCassandraExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

import static org.carlspring.gradle.plugins.embedded.cassandra.EmbeddedCassandraPlugin.JVM_AUTOFIX_MESSAGE;

public abstract class StartCassandraTask
        extends DefaultTask
{

    private final Project project;

    private final EmbeddedCassandraExtension container;

    @Inject
    public StartCassandraTask(Project project)
    {
        //this.container = container;
        this.project = project;
        this.container = project.getExtensions().getByType(EmbeddedCassandraExtension.class);
    }

    @Input
    public abstract Property<Cassandra> getInstance();

    @TaskAction
    public void startCassandraDatabase()
    {
        System.out.println("Starting Cassandra in " + getInstance().get().getWorkingDirectory());

        Cassandra server = getInstance().getOrNull();

        if (server == null) {
            throw new RuntimeException("No instance to start.");
        }

        // Named as jvm15 but copied as jvm11 on purpose, because Cassandra doesn't know anything about JDK >= 11 yet so naming it differently
        // will not work.
        int jdkVersion = Runtime.version().feature();
        if (jdkVersion >= 15 && jdkVersion < 18) {
            System.out.println("[WARNING] You have requested Cassandra v4 which officially supports only for JDK 8 and 11. However you are " +
                               "about to run this build using " + Runtime.version().toString() + " which is not officially supported yet.");

            if(container.getCassandra4JVMAutoFix()) {
                System.out.println(JVM_AUTOFIX_MESSAGE);
                System.out.println("[WARNING] To disable this set cassandra { cassandra4JDKAutoFix = false } in the DSL\n");
            }
        }
        else if (jdkVersion == 18) {
            String msg = "Cassandra needs SecurityManager which has been removed in JDK >= 18 -- your version is " + Runtime.version().toString();
            throw new RuntimeException(msg);
        }

        server.start();
    }

}
