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
import java.util.Date;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class Dumper extends MavenReporter {
    @Override
    public boolean preExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener) throws InterruptedException, IOException {
        if (isUnitTest(mojo))
            dumpClasspath(pom,listener);
        return super.preExecute(build, pom, mojo, listener);
    }

    @Override
    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {
        if (isUnitTest(mojo))
            dumpClasspath(pom,listener);
        return super.postExecute(build, pom, mojo, listener, error);
    }

    private boolean isUnitTest(MojoInfo mojo) {
        return mojo.is("org.apache.maven.plugins", "maven-surefire-plugin", "test");
    }

    private void dumpClasspath(MavenProject pom, BuildListener listener) {
        try {
            for (String path : (List<String>)pom.getTestClasspathElements()) {
                File f = new File(path);
                listener.getLogger().println("[HUDSON] "+new Date(f.lastModified()).toLocaleString()+" : "+path);
            }
        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace(listener.error("Failed to dump classpath"));
        }
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
