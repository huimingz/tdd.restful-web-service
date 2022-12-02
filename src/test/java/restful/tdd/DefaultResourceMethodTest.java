package restful.tdd;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class DefaultResourceMethodTest {

    private ResourceContext context;
    private UriInfoBuilder builder;
    private CallableResourceMethods resource;
    private UriInfo uriInfo;
    private MultivaluedHashMap<String, String> parameters;

    @BeforeEach
    public void before() {
        context = Mockito.mock(ResourceContext.class);
        uriInfo = Mockito.mock(UriInfo.class);
        builder = Mockito.mock(UriInfoBuilder.class);
        resource = Mockito.mock(CallableResourceMethods.class);
        parameters = new MultivaluedHashMap<>();

        Mockito.when(builder.getLastMatchedResource()).thenReturn(resource);
        Mockito.when(builder.createUriInfo()).thenReturn(uriInfo);
        Mockito.when(uriInfo.getPathParameters()).thenReturn(parameters);
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(parameters);
    }

    @Test
    public void should_call_resource_method() throws NoSuchMethodException {
        Mockito.when(resource.get()).thenReturn("resource called");
        DefaultResourceMethod resourceMethod = getResourceMethod("get");

        Assertions.assertEquals(new GenericEntity<>("resource called", String.class), resourceMethod.call(context, builder));
    }

    private static DefaultResourceMethod getResourceMethod(String methodName, Class... types) throws NoSuchMethodException {
        return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(methodName, types));
    }

    @Test
    public void should_inject_string_to_path_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getPathParam", String.class);

        parameters.put("path", List.of("path"));
        resourceMethod.call(context, builder);

        Mockito.verify(resource).getPathParam("path");
    }

    @Test
    public void should_call_resource_method_with_void_return() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getVoid");

        Assertions.assertNull(resourceMethod.call(context, builder));
    }

    @Test
    public void should_inject_int_to_path_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getPathParam", int.class);

        parameters.put("path", List.of("1"));
        resourceMethod.call(context, builder);

        Mockito.verify(resource).getPathParam(1);
    }
    @Test
    public void should_inject_string_to_query_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getQueryParam", String.class);

        parameters.put("path", List.of("query"));
        resourceMethod.call(context, builder);

        Mockito.verify(resource).getQueryParam("query");
    }

    @Test
    public void should_inject_int_to_query_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getQueryParam", int.class);

        parameters.put("path", List.of("1"));
        resourceMethod.call(context, builder);

        Mockito.verify(resource).getQueryParam(1);
    }

    // TODO: return type

    interface CallableResourceMethods {
        @GET
        String get();

        @POST
        void getVoid();

        @GET
        List<String> getList();

        @GET
        String getPathParam(@PathParam("path") String value);

        @GET
        String getPathParam(@PathParam("path") int value);

        @GET
        String getQueryParam(@QueryParam("path") String value);

        @GET
        String getQueryParam(@QueryParam("path") int value);
    }
}
