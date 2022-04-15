package org.carlspring.gradle.plugins.embedded.cassandra.tasks;

import com.github.nosan.embedded.cassandra.Cassandra;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public abstract class StopCassandraTask
        extends DefaultTask
{

    @Input
    public abstract Property<Cassandra> getInstance();

    @TaskAction
    public void stopCassandraDatabase()
    {
        Cassandra server = getInstance().getOrNull();

        if (server != null) {
            try {
                String running = getInstance().get().isRunning() ? "yes" : "no";
                String workdir = getInstance().get().getWorkingDirectory().toString();
                System.out.println("Stopping Cassandra in " + workdir + " (is running: " + running + ")");
                System.out.println();
                server.stop();
            }
            catch (Exception e) {
                // nothing we can really do.
                System.out.println("[ERROR] An error occurred while stopping Cassandra!");
                e.printStackTrace();
            }
        }

    }

}
