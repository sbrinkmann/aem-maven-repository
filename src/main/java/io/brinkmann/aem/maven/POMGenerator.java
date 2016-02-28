package io.brinkmann.aem.maven;

import io.brinkmann.aem.maven.model.ArtifactInformation;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * Created by sbrinkmann on 13.02.16.
 */
public interface POMGenerator {

    String generatePOM(BundleContext bundleContext, String groupId, String artifactId, String version) throws IOException;

    String generateDependenciesPomFragment(BundleContext bundleContext, String dependencyOutputPrefix) throws IOException;

    Set<ArtifactInformation> getGeneratedDependencyList(BundleContext bundleContext) throws IOException;
}
