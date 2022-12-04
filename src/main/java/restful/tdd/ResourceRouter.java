package restful.tdd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource extends UriHandler {
        Optional<ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder);
    }

    interface ResourceMethod extends UriHandler {
        GenericEntity<?> call(ResourceContext context, UriInfoBuilder builder);

        String getHttpMethod();
    }
}

class DefaultResourceRouter implements ResourceRouter {

    private Runtime runtime;
    private List<Resource> resources;

    public DefaultResourceRouter(Runtime runtime, List<Resource> resources) {
        this.runtime = runtime;
        this.resources = resources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();
        UriInfoBuilder uri = runtime.createUriInfoBuilder(request);

        List<Resource> resources_ = resources;
        Optional<ResourceMethod> method = UriHandlers.mapMatched(path, resources_, (result, resource) -> findResourceMethod(request, resourceContext, uri, result, resource));

        if (method.isEmpty()) {
            return (OutboundResponse) Response.status(Response.Status.NOT_FOUND).build();
        }

        return (OutboundResponse) method.map(m -> m.call(resourceContext, uri)).map(entity -> {
            return (entity.getEntity() instanceof OutboundResponse) ? (OutboundResponse) entity.getEntity() : Response.ok(entity).build();
        }).orElseGet(() -> Response.noContent().build());
    }

    private static Optional<ResourceMethod> findResourceMethod(HttpServletRequest request, ResourceContext resourceContext, UriInfoBuilder uri, Optional<UriTemplate.MatchResult> matched, Resource handler) {
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

    public Optional<ResourceRouter.ResourceMethod> findResourceMethods(String path, String method) {
        return findMethod(path, method).or(() -> findAlternative(path, method));
    }

    private Optional<ResourceRouter.ResourceMethod> findAlternative(String path, String method) {
        if (HttpMethod.HEAD.equals(method)) return findMethod(path, HttpMethod.GET).map(HeadResourceMethod::new);
        if (HttpMethod.OPTIONS.equals(method)) return Optional.of(new OptionResourceMethod(path));
        return Optional.empty();
    }

    private Optional<ResourceRouter.ResourceMethod> findMethod(String path, String method) {
        return Optional.ofNullable(resourceMethods.get(method)).flatMap(methods -> UriHandlers.match(path, methods, r -> r.getRemaining() == null));
    }

    class OptionResourceMethod implements ResourceRouter.ResourceMethod {

        private String path;

        public OptionResourceMethod(String path) {
            this.path = path;
        }

        @Override
        public GenericEntity<?> call(ResourceContext context, UriInfoBuilder builder) {
            return new GenericEntity<>(Response.noContent().allow(findAllowedMethods()).build(), Response.class);
        }

        private Set<String> findAllowedMethods() {
            Set<String> allowed = List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.HEAD, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.OPTIONS).stream().filter(m -> findMethod(path, m).isPresent()).collect(Collectors.toSet());

            allowed.add(HttpMethod.OPTIONS);
            if (allowed.contains(HttpMethod.GET)) allowed.add(HttpMethod.HEAD);

            return allowed;
        }

        @Override
        public String getHttpMethod() {
            return null;
        }

        @Override
        public UriTemplate getUriTemplate() {
            return null;
        }
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
        Object result = MethodInvoker.invoke(method, context, builder);
        return result != null ? new GenericEntity<>(result, method.getGenericReturnType()) : null;
    }


    interface ValueConverter<T> {
        T fromString(List<String> value);

        static <T> ValueConverter<T> singleValue(Function<String, T> converter) {
            return values -> converter.apply(values.get(0));
        }
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





class SubResourceLocators {
    private final List<ResourceRouter.Resource> resources;

    public SubResourceLocators(Method[] methods) {
        resources = Arrays.stream(methods).filter(m -> {
                    return m.isAnnotationPresent(Path.class) && Arrays.stream(m.getAnnotations()).noneMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class));
                })
                .map((Function<Method, ResourceRouter.Resource>) SubResourceLocator::new)
                .toList();
    }

    public Optional<ResourceRouter.ResourceMethod> findSubResourceMethods(String path, String method, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
        return UriHandlers.mapMatched(path, resources, (result, locator) -> locator.match(result.get(), method, mediaTypes, resourceContext, builder));
    }

    static class SubResourceLocator implements ResourceRouter.Resource {
        private PathTemplate uriTemplate;
        private Method method;

        public SubResourceLocator(Method method) {
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
        public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
            Object resource = builder.getLastMatchedResource();
            try {
                Object subResource = method.invoke(resource);
                return new RootResourceHandler(subResource, uriTemplate).match(result, httpMethod, mediaTypes, resourceContext, builder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class HeadResourceMethod implements ResourceRouter.ResourceMethod {
    ResourceRouter.ResourceMethod method;

    public HeadResourceMethod(ResourceRouter.ResourceMethod method) {
        this.method = method;
    }

    @Override
    public GenericEntity<?> call(ResourceContext context, UriInfoBuilder builder) {
        method.call(context, builder);
        return null;
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.HEAD;
    }

    @Override
    public UriTemplate getUriTemplate() {
        return this.method.getUriTemplate();
    }

    @Override
    public String toString() {
        return this.method.toString();
    }
}

class RootResourceHandler implements ResourceRouter.Resource {

    private SubResourceLocators subResourceLocators;
    private UriTemplate uriTemplate;
    private ResourceMethods resourceMethods;
    private Function<ResourceContext, Object> resource;

    public RootResourceHandler(Class<?> resourceClass) {
        this(resourceClass, new PathTemplate(getTemplate(resourceClass)), rc -> rc.getResource(resourceClass));
    }

    private static String getTemplate(Class<?> resourceClass) {
        if (!resourceClass.isAnnotationPresent(Path.class)) throw new IllegalArgumentException();
        return resourceClass.getAnnotation(Path.class).value();
    }

    public RootResourceHandler(Object resource, UriTemplate uriTemplate) {
        this(resource.getClass(), uriTemplate, rc -> resource);
    }


    private RootResourceHandler(Class<?> resourceClass, UriTemplate uriTemplate, Function<ResourceContext, Object> resource) {
        this.uriTemplate = uriTemplate;
        this.resourceMethods = new ResourceMethods(resourceClass.getMethods());
        this.subResourceLocators = new SubResourceLocators(resourceClass.getMethods());
        this.resource = resource;
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
        builder.addMatchedResult(resource.apply(resourceContext));
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        return resourceMethods.findResourceMethods(remaining, httpMethod)
                .or(() -> subResourceLocators.findSubResourceMethods(remaining, httpMethod, mediaTypes, resourceContext, builder));
    }

    @Override
    public UriTemplate getUriTemplate() {
        return this.uriTemplate;
    }

}
