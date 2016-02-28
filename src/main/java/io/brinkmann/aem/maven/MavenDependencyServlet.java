package io.brinkmann.aem.maven;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Created by sbrinkmann on 13.02.16.
 */
@Service(value = javax.servlet.Servlet.class)
@Component(immediate = true, metatype = true)
@Properties({
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true),
        @Property(name = "service.description", value = "Provides a POM file with a represents the dependencies of the Bundles running inside Apache Felix.", propertyPrivate = true),
        @Property(name = "sling.servlet.paths", value = "/bin/maven/dependencies")
})
public class MavenDependencyServlet extends SlingSafeMethodsServlet {
    @Reference
    POMGenerator pomGenerator;

    private BundleContext bundleContext = null;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String version = request.getParameter("version");
        String groupId = request.getParameter("groupId");
        String artifactId = request.getParameter("artifactId");

        boolean dependenciesOnly = "true".equals(request.getParameter("dependenciesOnly"));

        String responseContent;
        if(dependenciesOnly)
        {
            responseContent = pomGenerator.generateDependenciesPomFragment(bundleContext, "");
        }
        else
        {
            responseContent = pomGenerator.generatePOM(bundleContext, groupId, artifactId, version);
        }
        response.getWriter().write(responseContent);
    }

    protected final void activate(final ComponentContext context) {
        bundleContext = context.getBundleContext();
    }
}
