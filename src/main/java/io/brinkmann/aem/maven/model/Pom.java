package io.brinkmann.aem.maven.model;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by sbrinkmann on 14.02.16.
 */
public class Pom extends ArtifactInformation {

    Set<ArtifactInformation> dependencies = new TreeSet<ArtifactInformation>();

    public Set<ArtifactInformation> getDependencies() {
        return dependencies;
    }
}
