package io.brinkmann.aem.maven;

import io.brinkmann.aem.maven.exceptions.ArtifactDoesntExistInApacheFelix;
import io.brinkmann.aem.maven.exceptions.ArtifactInformationCannotBeResolvedException;
import io.brinkmann.aem.maven.model.ArtifactInformation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by sbrinkmann on 13.02.16.
 */
@Service(value = javax.servlet.Servlet.class)
@Component(immediate = true, metatype = true)
@Properties({
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true),
        @Property(name = "service.description", value = "Maven repository reflecting the bundles running in Apache Felix.", propertyPrivate = true),
})
public class MavenRepositoryServlet extends HttpServlet {
    Logger LOG = LoggerFactory.getLogger(MavenRepositoryServlet.class);

    @Property(name = "sling.servlet.paths", value = "/bin/maven/repository")
    private final static String PROP_REPOSITORY_SERVLET_PATH = "sling.servlet.paths";

    private String repositoryServletPath;

    @Reference
    private HttpService httpService;

    @Reference
    POMGenerator pomGenerator;

    private BundleContext bundleContext = null;

    @Override
    protected final void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        try {
            ArtifactInformation fromRequestResolvedArtifactInformation = extractArtifactInformationFromRequest(request);

            ArtifactInformation artifactFromApacheFelix = lookupArtifactInApacheFelix(fromRequestResolvedArtifactInformation);

            String fileExtension = FilenameUtils.getExtension(request.getRequestURI());

            switch (fileExtension) {
                case "pom":
                    response.setContentType("text/xml;charset=UTF-8");
                    response.getWriter().write(fromRequestResolvedArtifactInformation.getPomFile());
                    break;
                case "xml":
                    response.setContentType("text/xml;charset=UTF-8");
                    response.getWriter().write(fromRequestResolvedArtifactInformation.getMavenMetadata());
                    break;
                case "sha1":
                    response.getOutputStream().write(generateSha1Hash(artifactFromApacheFelix.getAssociatedBundle()));
                    break;
                case "jar":
                    response.setContentType("application/java-archive");
                    writeBundleArtifactFile(response.getOutputStream(), artifactFromApacheFelix.getAssociatedBundle());
                    break;
                default:
                    handleArtifactInformationCannotBeResolvedException(request.getRequestURI(), response.getWriter());
            }
        } catch (ArtifactDoesntExistInApacheFelix | ArtifactInformationCannotBeResolvedException | NoSuchAlgorithmException ex) {
            handleArtifactInformationCannotBeResolvedException(request.getRequestURI(), response.getWriter());
        }
    }

    private byte[] generateSha1Hash(Bundle associatedBundle) throws IOException, NoSuchAlgorithmException {
        MessageDigest hash = MessageDigest.getInstance("SHA1");
        hash.reset();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream output = new DigestOutputStream(byteArrayOutputStream, hash);

        writeBundleArtifactFile(output, associatedBundle);

        DigestOutputStream digestOutput = (DigestOutputStream) output;
        byte[] digest = digestOutput.getMessageDigest().digest();
        String hexStr = "";
        for (int i = 0; i < digest.length; i++) {
            hexStr += Integer.toString((digest[i] & 0xff) + 0x100, 16)
                    .substring(1);
        }

        return hexStr.getBytes();
    }

    private ArtifactInformation lookupArtifactInApacheFelix(ArtifactInformation fromRequestResolvedArtifactInformation) throws IOException, ArtifactDoesntExistInApacheFelix {
        ArtifactInformation artifactInformationFromApacheFelix = null;
        Set<ArtifactInformation> artifactsInApacheFelix = pomGenerator.getGeneratedDependencyList(bundleContext);
        for (ArtifactInformation artifactInApacheFelix : artifactsInApacheFelix) {
            if (artifactInApacheFelix.equals(fromRequestResolvedArtifactInformation)) {
                artifactInformationFromApacheFelix = artifactInApacheFelix;
                break;
            }
        }

        if (artifactInformationFromApacheFelix == null) {
            throw new ArtifactDoesntExistInApacheFelix();
        }

        return artifactInformationFromApacheFelix;
    }

    private void handleArtifactInformationCannotBeResolvedException(String requestURI, PrintWriter output) {
        output.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">");
        output.write("<html><head><title>Index of " + requestURI + "</title></head><body><h1>Index of " + requestURI + "</h1>");
        output.write("<pre><hr></pre></body></html>");
        output.flush();
    }

    private ArtifactInformation extractArtifactInformationFromRequest(final HttpServletRequest request) throws ArtifactInformationCannotBeResolvedException {
        ArtifactInformation artifactInformation = new ArtifactInformation();

        if(repositoryServletPath.length() == request.getRequestURI().length())
            throw new ArtifactInformationCannotBeResolvedException();

        String artifactRequestInformation = request.getRequestURI().substring(repositoryServletPath.length() + 1);
        List<String> artifactRequestInformationParts = Arrays.asList(artifactRequestInformation.split("/"));

        if (artifactRequestInformationParts.size() < 3)
            throw new ArtifactInformationCannotBeResolvedException();

        List<String> groupIdParts = artifactRequestInformationParts.subList(0, artifactRequestInformationParts.size() - 3);

        String groupId = StringUtils.join(groupIdParts, ".");
        String artifactId = artifactRequestInformationParts.get(artifactRequestInformationParts.size() - 3);
        String version = artifactRequestInformationParts.get(artifactRequestInformationParts.size() - 2);

        artifactInformation.setGroupId(groupId);
        artifactInformation.setArtifactId(artifactId);
        artifactInformation.setVersion(version);

        return artifactInformation;
    }

    private void writeBundleArtifactFile(OutputStream output, Bundle bundle) throws IOException {
        ArrayList<String> inZip = new ArrayList<>();
        Enumeration bundleResources = bundle.findEntries("/", null, true);
        if (bundleResources != null) {
            ZipOutputStream zipOutputStream = new ZipOutputStream(output);
            while (bundleResources.hasMoreElements()) {

                URL uriResourceToZip = (URL) bundleResources.nextElement();
                try {
                    String path = uriResourceToZip.getPath();
                    if (inZip.contains(path.substring(1))) {
                        continue;
                    }
                    ZipEntry zipEntry = new ZipEntry(path.substring(1));
                    zipEntry.setTime(bundle.getLastModified());
                    zipOutputStream.putNextEntry(zipEntry);
                    InputStream input = uriResourceToZip.openStream();
                    IOUtils.copy(input, zipOutputStream);
                    zipOutputStream.closeEntry();
                    input.close();
                    inZip.add(path.substring(1));
                } catch (Exception ex) {
                    LOG.warn("Issue while reading resource [" + uriResourceToZip + "]", ex);
                }
            }
            zipOutputStream.close();
        } else {
            throw new IOException("No bundle resource found");
        }
    }

    protected final void activate(ComponentContext componentContext) {
        bundleContext = componentContext.getBundleContext();

        final Dictionary<?, ?> properties = componentContext.getProperties();
        repositoryServletPath = (String) properties.get(PROP_REPOSITORY_SERVLET_PATH);

        try {
            httpService.registerServlet(repositoryServletPath, this, null, null);
        } catch (ServletException e) {
            LOG.error(e.getMessage());
        } catch (NamespaceException e) {
            LOG.error(e.getMessage());
        }
    }

    protected void deactivate(ComponentContext componentContext) {

        httpService.unregister(repositoryServletPath);
    }
}
