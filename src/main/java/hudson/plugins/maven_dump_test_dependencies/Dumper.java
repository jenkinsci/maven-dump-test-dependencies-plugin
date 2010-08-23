package hudson.plugins.maven_dump_test_dependencies;

import hudson.Extension;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MavenModule;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MojoInfo;
import hudson.model.BuildListener;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class Dumper extends MavenReporter {
    private transient Map<String,Long> last;


    @Override
    public boolean preExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener) throws InterruptedException, IOException {
        if (isUnitTest(mojo))
            last = dumpClasspath(pom,listener,null);
        return super.preExecute(build, pom, mojo, listener);
    }

    @Override
    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {
        if (isUnitTest(mojo))
            dumpClasspath(pom,listener,last);
        return super.postExecute(build, pom, mojo, listener, error);
    }

    private boolean isUnitTest(MojoInfo mojo) {
        return mojo.is("org.apache.maven.plugins", "maven-surefire-plugin", "test");
    }

    private Map<String,Long> dumpClasspath(MavenProject pom, BuildListener listener, Map<String,Long> baseline) {
        if (baseline==null) baseline = Collections.emptyMap();
        Map<String,Long> data = new HashMap<String, Long>();
        try {
            for (String path : (List<String>)pom.getTestClasspathElements()) {
                File f = new File(path);
                long l = f.lastModified();
                String timestamp = new Date(l).toLocaleString();
                if (Long.valueOf(l).equals(baseline.get(path)))
                    timestamp = "identical";
                listener.getLogger().println("[HUDSON] "+ timestamp +" : "+path);
                data.put(path,l);
            }
        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace(listener.error("Failed to dump classpath"));
        }
        return data;
    }

    @Extension
    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public String getDisplayName() {
            return "Dump Test Classpath";
        }

        public Dumper newAutoInstance(MavenModule module) {
            return new Dumper();
        }
    }

    private static final long serialVersionUID = 1L;
}
