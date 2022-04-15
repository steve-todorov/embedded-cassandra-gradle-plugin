package org.carlspring.gradle.plugins.embedded.cassandra.tasks;

import com.github.nosan.embedded.cassandra.Cassandra;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public abstract class StartCassandraTask
        extends DefaultTask
{

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

        server.start();
    }

}
