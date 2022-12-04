package restful.tdd;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubResourceLocatorTest {

    SubResourceMethods resource;

    private DefaultResourceMethodTest.LastCall lastCall;
    private UriTemplate.MatchResult result;
    SomeServiceInContext service;
    private ResourceContext context;
    private UriInfo uriInfo;
    private UriInfoBuilder builder;
    private MultivaluedHashMap<String, String> parameters;

    record LastCall(String name, List<Object> arguments) {
    }

    @BeforeEach
    public void before() {
        resource = (SubResourceMethods) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{SubResourceMethods.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    lastCall = new DefaultResourceMethodTest.LastCall(
                            getMethodName(name, Arrays.stream(method.getParameters()).map(p -> p.getType()).toList()),
                            args != null ? List.of(args) : List.of());
                    return new Message();
                });
        result = Mockito.mock(UriTemplate.MatchResult.class);

        context = Mockito.mock(ResourceContext.class);
        uriInfo = Mockito.mock(UriInfo.class);
        builder = Mockito.mock(UriInfoBuilder.class);
        service = Mockito.mock(SomeServiceInContext.class);
        parameters = new MultivaluedHashMap<>();

        Mockito.when(builder.getLastMatchedResource()).thenReturn(resource);
        Mockito.when(builder.createUriInfo()).thenReturn(uriInfo);
        Mockito.when(uriInfo.getPathParameters()).thenReturn(parameters);
//        Mockito.when(uriInfo.getQueryParameters()).thenReturn(parameters);
//        Mockito.when(context.getResource(eq(SomeServiceInContext.class))).thenReturn(service);
    }

    private static String getMethodName(String name, List<? extends Class<?>> classStream) {
        return name + "(" + classStream.stream().map(t -> t.getSimpleName()).collect(Collectors.joining(",")) + ")";
    }

    @Test
    public void should_inject_path_parameter_to_sub_resource_method() throws NoSuchMethodException {
        Method method = SubResourceMethods.class.getMethod("getPathParam", String.class);
        SubResourceLocators.SubResourceLocator locator = new SubResourceLocators.SubResourceLocator(method);

        parameters.put("param", List.of("path"));
        locator.match(result, "GET", new String[]{}, context, builder);

        Assertions.assertEquals("getPathParam(String)", lastCall.name());
        Assertions.assertEquals(List.of("path"), lastCall.arguments());
    }

    interface SubResourceMethods {
        @Path("/messages/{param}")
        Message getPathParam(@PathParam("param") String path);
    }

    static class Message {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }

    }
}
