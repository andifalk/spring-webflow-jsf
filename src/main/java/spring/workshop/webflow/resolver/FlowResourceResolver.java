package spring.workshop.webflow.resolver;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.view.facelets.ResourceResolver;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.faces.webflow.Jsf2FlowResourceResolver;
import org.springframework.faces.webflow.JsfUtils;
import org.springframework.util.ClassUtils;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;

/**
 * Resolves Facelets templates using Spring Resource paths such as "classpath:foo.xhtml". Configure it via a context
 * parameter in web.xml:
 *
 * <pre>
 * &lt;context-param/&gt;
 * 	&lt;param-name&gt;facelets.RESOURCE_RESOLVER&lt;/param-name&gt;
 * 	&lt;param-value&gt;org.springframework.faces.webflow.FlowResourceResolver&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 *
 */
public class FlowResourceResolver extends ResourceResolver {

    /**
     * All known {@link ResourceResolver} implementations in the priority order
     */
    private static final List<String> RESOLVERS_CLASSES;
    static {
        List<String> resolvers = new ArrayList<String>();
        resolvers.add("com.sun.faces.facelets.impl.DefaultResourceResolver");
        resolvers.add("org.apache.myfaces.view.facelets.impl.DefaultResourceResolver");
        RESOLVERS_CLASSES = Collections.unmodifiableList(resolvers);
    }

    private final ResourceResolver delegateResolver;

    public FlowResourceResolver ( ResourceResolver delegateResolver ) {
        this.delegateResolver = delegateResolver;
    }

    public FlowResourceResolver() {
        this.delegateResolver = null; //createDelegateResolver();
    }

    private ResourceResolver createDelegateResolver() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            for (String resolverClass : RESOLVERS_CLASSES) {
                if (ClassUtils.isPresent(resolverClass, classLoader)) {
                    return (ResourceResolver) ClassUtils.forName(resolverClass, classLoader).newInstance();
                }
            }
        } catch (Exception e) {
        }
        throw new IllegalStateException("Unable to find Default ResourceResolver");
    }

    public URL resolveUrl(String path) {

        if (!JsfUtils.isFlowRequest()) {
            return this.delegateResolver.resolveUrl(path);
        }

        try {
            RequestContext context = RequestContextHolder.getRequestContext();
            ApplicationContext flowContext = context.getActiveFlow().getApplicationContext();
            if (flowContext == null) {
                throw new IllegalStateException("A Flow ApplicationContext is required to resolve Flow View Resources");
            }

            ApplicationContext appContext = flowContext.getParent();
            Resource viewResource = appContext.getResource(path);
            if (viewResource.exists()) {
                return viewResource.getURL();
            } else {
                return this.delegateResolver.resolveUrl(path);
            }
        } catch (IOException ex) {
            throw new FacesException(ex);
        }
    }

}