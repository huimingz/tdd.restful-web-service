package restful.tdd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.eq;

public class ResourceDispatcherTest {
    private RuntimeDelegate delegate;

    private Runtime runtime;

    @BeforeEach
    public void before() {
        runtime = Mockito.mock(Runtime.class);
        delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        Mockito.when(delegate.createResponseBuilder()).thenReturn(new Response.ResponseBuilder() {
            private Object entity;
            private int status;

            @Override
            public Response build() {
                OutboundResponse response = Mockito.mock(OutboundResponse.class);
                Mockito.when(response.getEntity()).thenReturn(this.entity);
                return response;
            }

            @Override
            public Response.ResponseBuilder clone() {
                return null;
            }

            @Override
            public Response.ResponseBuilder status(int status) {
                return null;
            }

            @Override
            public Response.ResponseBuilder status(int status, String reasonPhrase) {
                this.status = status;
                return this;
            }

            @Override
            public Response.ResponseBuilder entity(Object entity) {
                this.entity = entity;
                return this;
            }

            @Override
            public Response.ResponseBuilder entity(Object entity, Annotation[] annotations) {
                return null;
            }

            @Override
            public Response.ResponseBuilder allow(String... methods) {
                return null;
            }

            @Override
            public Response.ResponseBuilder allow(Set<String> methods) {
                return null;
            }

            @Override
            public Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
                return null;
            }

            @Override
            public Response.ResponseBuilder encoding(String encoding) {
                return null;
            }

            @Override
            public Response.ResponseBuilder header(String name, Object value) {
                return null;
            }

            @Override
            public Response.ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
                return null;
            }

            @Override
            public Response.ResponseBuilder language(String language) {
                return null;
            }

            @Override
            public Response.ResponseBuilder language(Locale language) {
                return null;
            }

            @Override
            public Response.ResponseBuilder type(MediaType type) {
                return null;
            }

            @Override
            public Response.ResponseBuilder type(String type) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variant(Variant variant) {
                return null;
            }

            @Override
            public Response.ResponseBuilder contentLocation(URI location) {
                return null;
            }

            @Override
            public Response.ResponseBuilder cookie(NewCookie... cookies) {
                return null;
            }

            @Override
            public Response.ResponseBuilder expires(Date expires) {
                return null;
            }

            @Override
            public Response.ResponseBuilder lastModified(Date lastModified) {
                return null;
            }

            @Override
            public Response.ResponseBuilder location(URI location) {
                return null;
            }

            @Override
            public Response.ResponseBuilder tag(EntityTag tag) {
                return null;
            }

            @Override
            public Response.ResponseBuilder tag(String tag) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variants(Variant... variants) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variants(List<Variant> variants) {
                return null;
            }

            @Override
            public Response.ResponseBuilder links(Link... links) {
                return null;
            }

            @Override
            public Response.ResponseBuilder link(URI uri, String rel) {
                return null;
            }

            @Override
            public Response.ResponseBuilder link(String uri, String rel) {
                return null;
            }
        });
    }

    @Test
    public void should() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ResourceContext context = Mockito.mock(ResourceContext.class);

        Mockito.when(request.getServletPath()).thenReturn("/users");
        Mockito.when(context.getResource(eq(Users.class))).thenReturn(new Users());

        Router router = new Router(runtime, List.of(new ResourceClass(Users.class)));

        OutboundResponse response = router.dispatch(request, context);
        GenericEntity<String> entity = (GenericEntity<String>) response.getEntity();
        Assertions.assertEquals("all", entity.getEntity());
    }

    static class Router implements ResourceRouter {

        private Runtime runtime;
        private List<Resource> rootResource;

        public Router(Runtime runtime, List<Resource> rootResource) {
            this.runtime = runtime;
            this.rootResource = rootResource;
        }

        @Override
        public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
//            UriInfoBuilder builder = runtime.createUriBuilder(request);
            ResourceMethod resourceMethod = rootResource.stream().map(r -> r.matches(request.getServletPath(), new String[0], null)).filter(o -> o.isPresent()).findFirst().get().get();
            try {
                GenericEntity entity = resourceMethod.call(resourceContext, null);
                return (OutboundResponse) Response.ok(entity).build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class ResourceClass implements Resource {
        private final Pattern pattern;
        private final String path;
        private Class<?> resourceClass;
        private Map<URITemplate, ResourceMethod> methods = new HashMap<>();

        record URITemplate(Pattern uri, String[] mediaTypes) {
        }

        public ResourceClass(Class<?> resourceClass) {
            this.resourceClass = resourceClass;
            path = resourceClass.getAnnotation(Path.class).value();
            pattern = Pattern.compile(path + "(/.*)?");

            for (Method method : Arrays.stream(resourceClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).toList()) {
                methods.put(new URITemplate(pattern, method.getAnnotation(Produces.class).value()), new NormalResourceMethod(resourceClass, method));
            }
        }

        @Override
        public Optional<ResourceMethod> matches(String path, String[] mediaTypes, UriInfoBuilder builder) {
            if (!pattern.matcher(path).matches()) {
                return Optional.empty();
            }
            return methods.entrySet().stream().filter(e -> e.getKey().uri.matcher(path).matches()).map(e -> e.getValue()).findFirst();
        }
    }

    static class NormalResourceMethod implements ResourceMethod {
        private Class<?> resourceClass;
        private Method method;

        public NormalResourceMethod(Class<?> resourceClass, Method method) {
            this.resourceClass = resourceClass;
            this.method = method;
        }

        @Override
        public GenericEntity<?> call(ResourceContext context, UriInfoBuilder builder) {
            Object resource = context.getResource(resourceClass);

            try {
                return new GenericEntity<>(method.invoke(resource), method.getGenericReturnType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    interface Resource {
        Optional<ResourceMethod> matches(String path, String[] mediaTypes, UriInfoBuilder builder);
    }

    interface ResourceMethod {
        GenericEntity<?> call(ResourceContext context, UriInfoBuilder builder);
    }

    interface UriInfoBuilder {
        void pushMatchedPath(String path);

        void addParameter(String name, String value);
    }

    @Path("/users")
    static class Users {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String asText() {
            return "all";
        }
    }
}
