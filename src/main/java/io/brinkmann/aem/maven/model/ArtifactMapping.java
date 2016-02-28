package io.brinkmann.aem.maven.model;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sbrinkmann on 14.02.16.
 */
public class ArtifactMapping extends ArtifactInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactMapping.class);

    private static final Pattern osgiConfigPattern = Pattern.compile("([^=]+)=([^:]+):?([^:]+)?:?(.*)");

    private String bundleSymbolicName;

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public void setBundleSymbolicName(String bundleSymbolicName) {
        this.bundleSymbolicName = bundleSymbolicName;
    }

    public static ArtifactMapping parseMappingFromOsgiConfig(String osgiConfigString) {
        ArtifactMapping parsedArtifactMapping = null;

        if (StringUtils.isNotEmpty(osgiConfigString)) {
            Matcher osgiConfigParameters = osgiConfigPattern.matcher(osgiConfigString);
            if (osgiConfigParameters.matches()) {
                parsedArtifactMapping = new ArtifactMapping();
                parsedArtifactMapping.bundleSymbolicName = osgiConfigParameters.group(1);
                parsedArtifactMapping.setGroupId(osgiConfigParameters.group(2));
                parsedArtifactMapping.setArtifactId(osgiConfigParameters.group(3));
                parsedArtifactMapping.setVersion(osgiConfigParameters.group(4));
            } else {
                LOGGER.warn("OSGi config string doesn't match: " + osgiConfigString);
            }
        } else {
            LOGGER.warn("Cannot parse empty OSGi config string.");
        }

        return parsedArtifactMapping;
    }
}
