package io.brinkmann.aem.maven.model;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;

import java.io.StringWriter;


/**
 * Created by sbrinkmann on 14.02.16.
 */
public class ArtifactInformation implements Comparable<ArtifactInformation> {

    private String groupId = "";

    private String artifactId = "";

    private String version = "";

    private String artifactComment = "";

    private Bundle associatedBundle;

    public ArtifactInformation() {

    }

    public ArtifactInformation(String artifactComment) {
        this.artifactComment = artifactComment;
    }

    public ArtifactInformation(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public ArtifactInformation(String groupId, String artifactId, String version, String artifactComment) {
        this(groupId, artifactId, version);
        this.artifactComment = artifactComment;
    }

    public ArtifactInformation(String groupId, String artifactId, String version, String artifactComment, Bundle associatedBundle) {
        this(groupId, artifactId, version, artifactComment);
        this.associatedBundle = associatedBundle;
    }

    /**
     * @return true
     */
    public boolean isEmpty() {
        return StringUtils.isEmpty(groupId) && StringUtils.isEmpty(artifactId);
    }

    public String getArtifactIdentifier() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDependencyPomFragment(boolean scopeProvided, String outputPrefix) {
        outputPrefix = StringUtils.isNotEmpty(outputPrefix) ? outputPrefix : "";

        StringWriter dependenciesPomFragment = new StringWriter();
        if (StringUtils.isNotEmpty(artifactComment)) {
            dependenciesPomFragment.write(outputPrefix + "<!-- " + artifactComment + " -->\n");
        }
        if (StringUtils.isNotEmpty(groupId) && StringUtils.isNotEmpty(artifactId)) {
            dependenciesPomFragment.write(outputPrefix + "<dependency>\n");
            dependenciesPomFragment.write(outputPrefix + "\t<groupId>" + groupId + "</groupId>\n");
            dependenciesPomFragment.write(outputPrefix + "\t<artifactId>" + artifactId + "</artifactId>\n");
            if (StringUtils.isNotEmpty(version)) {
                dependenciesPomFragment.write(outputPrefix + "\t<version>" + version + "</version>\n");
            }
            if (scopeProvided) {
                dependenciesPomFragment.write(outputPrefix + "\t<scope>provided</scope>\n");
            }
            dependenciesPomFragment.write(outputPrefix + "</dependency>\n");
        }
        return dependenciesPomFragment.toString();
    }

    public String getPomFile() {
        StringWriter pomFile = new StringWriter();

        pomFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        pomFile.write("<project>\n");
        pomFile.write("\t<modelVersion>4.0.0</modelVersion>\n");
        pomFile.write("\t<groupId>" + getGroupId() + "</groupId>\n");
        pomFile.write("\t<artifactId>" + getArtifactId() + "</artifactId>\n");
        pomFile.write("\t<version>" + getVersion() + "</version>\n");
        pomFile.write("\t<dependencies/>\n");
        pomFile.write("</project>\n");

        return pomFile.toString();
    }

    public String getMavenMetadata() {
        StringWriter mavenMetadata = new StringWriter();

        mavenMetadata.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        mavenMetadata.write("<metadata ");
        mavenMetadata.write("xsi:schemaLocation=\"http://maven.apache.org/METADATA/1.0.0 http://maven.apache.org/xsd/metadata-1.0.0.xsd\" ");
        mavenMetadata.write("xmlns=\"http://maven.apache.org/METADATA/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        mavenMetadata.write("<groupId>" + getGroupId() + "</groupId>\n");
        mavenMetadata.write("<artifactId>" + getArtifactId() + "</artifactId>\n");
        mavenMetadata.write("<version>" + getVersion() + "</version>\n");
        mavenMetadata.write("</metadata>");

        return mavenMetadata.toString();
    }

    public Bundle getAssociatedBundle() {
        return associatedBundle;
    }

    public void setAssociatedBundle(Bundle associatedBundle) {
        this.associatedBundle = associatedBundle;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj instanceof ArtifactInformation) {
            ArtifactInformation otherArtifactInformation = (ArtifactInformation) obj;
            isEqual = getCompareAbleValue().equals(otherArtifactInformation.getCompareAbleValue());
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        return getCompareAbleValue().hashCode();
    }

    private String getCompareAbleValue() {
        if (!isEmpty()) {
            return getArtifactIdentifier();
        } else if (StringUtils.isNotEmpty(artifactComment)) {
            return artifactComment;
        }

        throw new RuntimeException("This reference has no compareable value");
    }

    public int compareTo(ArtifactInformation otherArtifactInformation) {
        int compareResult = getCompareAbleValue().compareTo(otherArtifactInformation.getCompareAbleValue());
        return compareResult;
    }
}
