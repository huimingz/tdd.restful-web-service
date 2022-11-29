package restful.tdd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> match(UriTemplate.MatchResult result, String method, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder);
    }

    interface RootResource extends Resource, UriHandler {
    }

    interface ResourceMethod extends UriHandler {
        GenericEntity<?> call(ResourceContext context, UriInfoBuilder builder);

        String getHttpMethod();
    }

    interface SubResourceLocator extends UriHandler {
        Resource getSubResource(ResourceContext resourceContext, UriInfoBuilder uriInfoBuilder);

    }
}

class DefaultResourceRouter implements ResourceRouter {

    private Runtime runtime;
    private List<ResourceRouter.RootResource> rootResources;

    public DefaultResourceRouter(Runtime runtime, List<RootResource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();
        UriInfoBuilder uri = runtime.createUriInfoBuilder(request);

        List<RootResource> rootResources_ = rootResources;
        Optional<ResourceMethod> method = UriHandlers.mapMatched(path, rootResources_, (result, resource) -> findResourceMethod(request, resourceContext, uri, result, resource));

        if (method.isEmpty()) {
            return (OutboundResponse) Response.status(Response.Status.NOT_FOUND).build();
        }

        return (OutboundResponse) method.map(m -> m.call(resourceContext, uri)).map(entity -> Response.ok(entity).build()).orElseGet(() -> Response.noContent().build());
    }

    private static Optional<ResourceMethod> findResourceMethod(HttpServletRequest request, ResourceContext resourceContext, UriInfoBuilder uri, Optional<UriTemplate.MatchResult> matched, RootResource handler) {
        return handler.match(matched.get(), request.getMethod(), Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), resourceContext, uri);
    }

}

class ResourceMethods {
    private Map<String, List<ResourceRouter.ResourceMethod>> resourceMethods;

    public ResourceMethods(Method[] methods) {
        this.resourceMethods = getResourceMethods(methods);
    }

    private static Map<String, List<ResourceRouter.ResourceMethod>> getResourceMethods(Method[] methods) {
        return Arrays.stream(methods)
                .filter(m -> Arrays.stream(m.getAnnotations()).anyMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(DefaultResourceMethod::new)
                .collect(Collectors.groupingBy(ResourceRouter.ResourceMethod::getHttpMethod));
    }

    public Optional<ResourceRouter.ResourceMethod> findResourceMethods(String method, String path) {
        return Optional.ofNullable(resourceMethods.get(method)).flatMap(methods -> UriHandlers.match(path, methods, r -> r.getRemaining() == null));
    }
}

class RootResourceClass implements ResourceRouter.RootResource {

    private SubResourceLocators subResourceLocators;
    private PathTemplate uriTemplate;
    private Class<?> resourceClass;
    private ResourceMethods resourceMethods;

    public RootResourceClass(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
        this.uriTemplate = new PathTemplate(resourceClass.getAnnotation(Path.class).value());
        Method[] methods = resourceClass.getMethods();
        this.resourceMethods = new ResourceMethods(methods);
        this.subResourceLocators = new SubResourceLocators(resourceClass.getMethods());
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String method, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
        builder.addMatchedResult(resourceContext.getResource(resourceClass));
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        return resourceMethods.findResourceMethods(method, remaining)
                .or(() -> subResourceLocators.findSubResourceMethods(remaining, method, mediaTypes, resourceContext, builder));
    }

    @Override
    public UriTemplate getUriTemplate() {
        return this.uriTemplate;
    }

}


class DefaultResourceMethod implements ResourceRouter.ResourceMethod {

    private String httpMethod;
    private UriTemplate uriTemplate;
    private Method method;

    public DefaultResourceMethod(Method method) {
        this.method = method;
        this.uriTemplate = new PathTemplate(Optional.ofNullable(method.getAnnotation(Path.class)).map(a -> a.value()).orElse(""));
        this.httpMethod = Arrays.stream(method.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(HttpMethod.class))
                .findFirst().get().annotationType().getAnnotation(HttpMethod.class).value();
    }

    @Override
    public GenericEntity<?> call(ResourceContext context, UriInfoBuilder builder) {
        return null;
    }

    @Override
    public String toString() {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    @Override
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }
}

class SubResource implements ResourceRouter.Resource {

    private ResourceMethods resourceMethods;
    private Object subResource;

    public SubResource(Object subResource) {
        this.subResource = subResource;
        this.resourceMethods = new ResourceMethods(subResource.getClass().getMethods());
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String method, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        return resourceMethods.findResourceMethods(method, remaining);
    }
}


class SubResourceLocators {
    private final List<ResourceRouter.SubResourceLocator> subResourceLocators;

    public SubResourceLocators(Method[] methods) {
        subResourceLocators = Arrays.stream(methods).filter(m -> {
                    return m.isAnnotationPresent(Path.class) && Arrays.stream(m.getAnnotations()).noneMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class));
                })
                .map((Function<Method, ResourceRouter.SubResourceLocator>) DefaultSubResourceLocator::new)
                .toList();
    }

    public Optional<ResourceRouter.SubResourceLocator> findSubResource(String path) {
        return UriHandlers.match(path, subResourceLocators);
    }

    public Optional<ResourceRouter.ResourceMethod> findSubResourceMethods(String path, String method, String[] mediaTypes, ResourceContext context, UriInfoBuilder builder) {
        return UriHandlers.mapMatched(path, subResourceLocators, (result, locator) -> locator.getSubResource(context, builder).match(result.get(), method, mediaTypes, context, builder));
    }

    static class DefaultSubResourceLocator implements ResourceRouter.SubResourceLocator {
        private PathTemplate uriTemplate;
        private Method method;

        public DefaultSubResourceLocator(Method method) {
            this.method = method;
            this.uriTemplate = new PathTemplate(method.getAnnotation(Path.class).value());
        }

        @Override
        public UriTemplate getUriTemplate() {
            return uriTemplate;
        }

        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }

        @Override
        public ResourceRouter.Resource getSubResource(ResourceContext resourceContext, UriInfoBuilder uriInfoBuilder) {
            Object resource = uriInfoBuilder.getLastMatchedResource();
            try {
                Object subResource = method.invoke(resource);
                uriInfoBuilder.addMatchedResult(subResource);
                return new SubResource(subResource);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}