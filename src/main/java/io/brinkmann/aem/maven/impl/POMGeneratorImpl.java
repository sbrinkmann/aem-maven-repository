package io.brinkmann.aem.maven.impl;

import io.brinkmann.aem.maven.POMGenerator;
import io.brinkmann.aem.maven.model.ArtifactInformation;
import io.brinkmann.aem.maven.model.ArtifactMapping;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sbrinkmann on 13.02.16.
 */
@Service(value = io.brinkmann.aem.maven.POMGenerator.class)
@Component(immediate = true, metatype = true, label = "POM File Generator", description = "Generates a POM file containing a list of dependencies which resolve a list of Maven dependencies to develop against this AEM version.")
public class POMGeneratorImpl implements POMGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(POMGeneratorImpl.class);

    private Map<String, ArtifactMapping> artifactMappings = new HashMap<String, ArtifactMapping>();

    private Set<Pattern> bundlesToBeIgnored = new HashSet<Pattern>();

    private String dependencyOutputPrefix = "";

    private String defaultGroupId;

    private String defaultArtifactId;

    private String defaultVersion;

    private boolean scopeProvided = true;

    @Property(value = {""}, unbounded = PropertyUnbounded.ARRAY, label = "Dependency Bundle Mapping", description = "Optional mapping from bundles to their maven dependency. Syntax: '<bundleSymbolicName>=<groupId>:<artifactId>:<version>' <artifactId> and <version> are optional and will be applied from the bundle symbolic name or bundle version in case they're not specified here.")
    private static final String PROP_DEPENDENCY_MAPPING = "dependencyBundleMapping";

    @Property(value = {""}, unbounded = PropertyUnbounded.ARRAY, label = "List Bundles to be Ignored", description = "RegEx pattern of bundles which shall be ignored from export.")
    private static final String PROP_IGNORE_BUNDLE = "listIgnoreBundle";

    @Property(value = "            ", label = "Dependency Output Prefix", description = "Spacing prefix for dependency block output")
    private static final String PROP_DEPENDENCY_OUTPUT_PREFIX = "dependencyOutputPrefix";

    @Property(value = "com.adobe.aem", label = "Default Parent Pom GroupId", description = "Will be applied in case it's not overwritten in the pom generation request.")
    private static final String PROP_DEFAULT_GROUP_ID = "defaultGroupId";

    @Property(value = "base-pom", label = "Default Parent Pom Artifact ID", description = "Will be applied in case it's not overwritten in the pom generation request.")
    private static final String PROP_DEFAULT_ARTIFACT_ID = "defaultArtifactId";

    @Property(value = "1.0", label = "Default Parent Pom Version", description = "Will be applied in case it's not overwritten in the pom generation request.")
    private static final String PROP_DEFAULT_VERSION = "defaultVersion";

    @Property(boolValue = true, label = "Depenencies Scope Provided", description = "Is this checkbox set, the dependencies will be exported with the scope provided")
    private static final String PROP_DEPENDENCY_SCOPE_PROVIDED = "depencyScopeProvided";
    private static final boolean PROP_DEPENDENCY_SCOPE_PROVIDED_DEFAULT_VALUE = true;

    @Activate
    protected void activateComponent(ComponentContext componentContext) {
        artifactMappings.clear();
        bundlesToBeIgnored.clear();

        final Dictionary<?, ?> properties = componentContext.getProperties();
        final String[] dependencyMappingList = (String[]) properties.get(PROP_DEPENDENCY_MAPPING);
        for (String dependencyMapping : dependencyMappingList) {
            ArtifactMapping artifactMapping = ArtifactMapping.parseMappingFromOsgiConfig(dependencyMapping);
            artifactMappings.put(artifactMapping.getBundleSymbolicName(), artifactMapping);
        }
        final String[] listIgnoreBundle = (String[]) properties.get(PROP_IGNORE_BUNDLE);
        for (String ignoreBundle : listIgnoreBundle) {
            try {
                bundlesToBeIgnored.add(Pattern.compile(ignoreBundle));
            } catch (Exception ex) {
                LOGGER.error("Cannot parse bundle to ignore because RegEx is not valid.", ex);
            }
        }
        dependencyOutputPrefix = (String) properties.get(PROP_DEPENDENCY_OUTPUT_PREFIX);
        defaultGroupId = (String) properties.get(PROP_DEFAULT_GROUP_ID);
        defaultArtifactId = (String) properties.get(PROP_DEFAULT_ARTIFACT_ID);
        defaultVersion = (String) properties.get(PROP_DEFAULT_VERSION);
        scopeProvided = PropertiesUtil.toBoolean(properties.get(PROP_DEPENDENCY_SCOPE_PROVIDED), PROP_DEPENDENCY_SCOPE_PROVIDED_DEFAULT_VALUE);
    }

    public String generatePOM(BundleContext bundleContext, String groupId, String artifactId, String version) throws IOException {

        List<String> messageFormatValues = new ArrayList<String>();

        groupId = StringUtils.isNotEmpty(groupId) ? groupId : defaultGroupId;
        messageFormatValues.add(groupId);

        artifactId = StringUtils.isNoneEmpty(artifactId) ? artifactId : defaultArtifactId;
        messageFormatValues.add(artifactId);

        version = StringUtils.isNoneEmpty(version) ? version : defaultVersion;
        messageFormatValues.add(version);

        messageFormatValues.add(generateDependenciesPomFragment(bundleContext, null));

        InputStream basePomInputStream = this.getClass().getResource("basePom.xml").openStream();
        String basePomTemplate = IOUtils.toString(basePomInputStream);
        String basePomCompleted = MessageFormat.format(basePomTemplate, messageFormatValues.toArray());

        return basePomCompleted;
    }

    public String generateDependenciesPomFragment(BundleContext bundleContext, String dependencyOutputPrefix) throws IOException {
        dependencyOutputPrefix = dependencyOutputPrefix != null ? dependencyOutputPrefix : this.dependencyOutputPrefix;
        Set<ArtifactInformation> dependencyList = getGeneratedDependencyList(bundleContext);
        StringWriter pomDependencies = new StringWriter();
        for (ArtifactInformation dependency : dependencyList) {
            pomDependencies.write(dependency.getDependencyPomFragment(scopeProvided, dependencyOutputPrefix));
        }
        return pomDependencies.toString();
    }

    public Set<ArtifactInformation> getGeneratedDependencyList(BundleContext bundleContext) throws IOException {

        Set<ArtifactInformation> dependencies = new TreeSet<>();

        for (Bundle bundle : bundleContext.getBundles()) {
            Enumeration pomResourcesInBundle = bundle.findEntries("META-INF", "pom.properties", true);
            boolean bundleExportsPackages = bundle.getHeaders().get("Export-Package") != null;
            boolean bundleIsFragment = bundle.getHeaders().get("Fragment-Host") != null;
            if (ignoreBundleFromExport(bundle.getSymbolicName())) {
                LOGGER.trace("Ignore OSGi Bundle [" + bundle.getSymbolicName() + "] from export.");
            } else if (bundleIsFragment) {
                ArtifactInformation dependency = new ArtifactInformation(bundle.getSymbolicName() + " [skipped because it's a fragment]");
                dependencies.add(dependency);
            } else if (pomResourcesInBundle == null) {
                if (artifactMappings.containsKey(bundle.getSymbolicName())) {
                    ArtifactMapping artifactMapping = artifactMappings.get(bundle.getSymbolicName());

                    String version = StringUtils.isNotEmpty(artifactMapping.getVersion()) ? artifactMapping.getVersion() : bundle.getVersion().toString();
                    String artifactId = StringUtils.isNotEmpty(artifactMapping.getArtifactId()) ? artifactMapping.getArtifactId() : bundle.getSymbolicName();
                    String groupId = artifactMapping.getGroupId();
                    String artifactComment = bundle.getSymbolicName();

                    ArtifactInformation dependency = new ArtifactInformation(groupId, artifactId, version, artifactComment, bundle);
                    dependencies.add(dependency);
                } else {
                    ArtifactInformation dependency = new ArtifactInformation(bundle.getSymbolicName() + " [skipped because of missing pom properties]");
                    dependencies.add(dependency);
                }
            } else if (!bundleExportsPackages) {
                ArtifactInformation dependency = new ArtifactInformation(bundle.getSymbolicName() + " [skipped due to no exports]");
                dependencies.add(dependency);
            } else {
                while (pomResourcesInBundle.hasMoreElements()) {
                    URL pomResource = (URL) pomResourcesInBundle.nextElement();
                    java.util.Properties properties = new java.util.Properties();
                    properties.load(pomResource.openStream());

                    String version = properties.get("version").toString();
                    String artifactId = properties.get("artifactId").toString();
                    String groupId = properties.get("groupId").toString();
                    String artifactComment = bundle.getSymbolicName();

                    if (artifactMappings.containsKey(bundle.getSymbolicName())) {
                        ArtifactMapping artifactMapping = artifactMappings.get(bundle.getSymbolicName());

                        version = StringUtils.isNotEmpty(artifactMapping.getVersion()) ? artifactMapping.getVersion() : version;
                        artifactId = StringUtils.isNotEmpty(artifactMapping.getArtifactId()) ? artifactMapping.getArtifactId() : artifactId;
                        groupId = artifactMapping.getGroupId();
                    }

                    ArtifactInformation dependency = new ArtifactInformation(groupId, artifactId, version, artifactComment, bundle);
                    dependencies.add(dependency);
                }
            }
        }

        filterDuplicateArtifacts(dependencies);

        return dependencies;
    }

    /**
     * Looks for dependencies with the same artifact id and group id and removes the duplicates with the smaller version numbers
     *
     * @param dependenciesToBeFiltered goeas through the list and applies the described filter
     */
    private void filterDuplicateArtifacts(Set<ArtifactInformation> dependenciesToBeFiltered) {
        List<ArtifactInformation> dependenciesToBeRemoved = new ArrayList<>();

        for (ArtifactInformation dependencyToBeFiltered : dependenciesToBeFiltered) {
            boolean removeDependency = false;
            for (ArtifactInformation dependencyArtifact : dependenciesToBeFiltered) {
                removeDependency = dependencyArtifact.isArtifactAndGroupIdEqual(dependencyToBeFiltered) && dependencyArtifact.getMavenVersion().compareTo(dependencyToBeFiltered.getMavenVersion()) > 0;

                if (removeDependency) {
                    break;
                }
            }
            if (removeDependency) {
                dependenciesToBeRemoved.add(dependencyToBeFiltered);
                LOGGER.debug("Will remove dependency ["+ dependencyToBeFiltered.getArtifactIdentifier() +"] because there is an identical dependency with a higher version number.");
            }
        }
        dependenciesToBeFiltered.removeAll(dependenciesToBeRemoved);
    }


    private boolean ignoreBundleFromExport(String bundleArtifactName) {
        for (Pattern bundleToBeIgnored : bundlesToBeIgnored) {
            if (bundleToBeIgnored.matcher(bundleArtifactName).matches()) {
                LOGGER.debug("Bundle [" + bundleArtifactName + "] will be ignored because of matching ignore pattern [" + bundleToBeIgnored.pattern() + "]");
                return true;
            }
        }

        return false;
    }

}
