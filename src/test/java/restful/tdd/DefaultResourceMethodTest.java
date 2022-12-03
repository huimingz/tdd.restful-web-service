package restful.tdd;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultResourceMethodTest {

    private ResourceContext context;
    private UriInfoBuilder builder;
    private CallableResourceMethods resource;
    private UriInfo uriInfo;
    private MultivaluedHashMap<String, String> parameters;

    private LastCall lastCall;

    record LastCall(String name, List<Object> arguments) {
    }

    @BeforeEach
    public void before() {
        resource = (CallableResourceMethods) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{CallableResourceMethods.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    lastCall = new LastCall(
                            getMethodName(name, Arrays.stream(method.getParameters()).map(p -> p.getType()).toList()),
                            args != null ? List.of(args) : List.of());
                    return "getList".equals(method.getName()) ? new ArrayList<String>() : null;
                });

        context = Mockito.mock(ResourceContext.class);
        uriInfo = Mockito.mock(UriInfo.class);
        builder = Mockito.mock(UriInfoBuilder.class);
        parameters = new MultivaluedHashMap<>();

        Mockito.when(builder.getLastMatchedResource()).thenReturn(resource);
        Mockito.when(builder.createUriInfo()).thenReturn(uriInfo);
        Mockito.when(uriInfo.getPathParameters()).thenReturn(parameters);
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(parameters);
    }

    private static String getMethodName(String name, List<? extends Class<?>> classStream) {
        return name + "(" + classStream.stream().map(t -> t.getSimpleName()).collect(Collectors.joining(",")) + ")";
    }

    @Test
    public void should_call_resource_method() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("get");
        resourceMethod.call(context, builder);

        Assertions.assertEquals("get()", lastCall.name);
    }

    private static DefaultResourceMethod getResourceMethod(String methodName, Class... types) throws NoSuchMethodException {
        return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(methodName, types));
    }

    record InjectableTpeTestCase(Class<?> type, String string, Object value) {
    }

    @TestFactory
    public List<DynamicTest> injectableTypes() {
        List<DynamicTest> tests = new ArrayList<>();

        List<InjectableTpeTestCase> typeCases = List.of(
                new InjectableTpeTestCase(String.class, "string", "string"),
                new InjectableTpeTestCase(int.class, "1", 1),
                new InjectableTpeTestCase(Double.class, "3.14", 3.14),
                new InjectableTpeTestCase(float.class, "3.14", 3.14f),
                new InjectableTpeTestCase(short.class, "323", (short) 323),
                new InjectableTpeTestCase(byte.class, "42", (byte) 42),
                new InjectableTpeTestCase(boolean.class, "true", true),
                new InjectableTpeTestCase(BigDecimal.class, "314", new BigDecimal("314"))
        );

        List<String> paramTypes = List.of("getQueryParam", "getPathParam");

        for (String type : paramTypes) {
            for (InjectableTpeTestCase testCase : typeCases) {
                tests.add(DynamicTest.dynamicTest("should inject " + testCase.type.getSimpleName() + " to " + type, () -> {
                    verifyResourceMethodCalled(type, testCase.type, testCase.string, testCase.value);
                }));
            }
        }
        return tests;
    }


    private void verifyResourceMethodCalled(String method, Class<?> type, String paramValue, Object value) throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod(method, type);

        parameters.put("param", List.of(paramValue));
        resourceMethod.call(context, builder);

        Assertions.assertEquals(getMethodName(method, List.of(type)), lastCall.name);
        Assertions.assertEquals(List.of(value), lastCall.arguments);
    }

    @Test
    public void should_call_resource_method_with_void_return() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getVoid");

        Assertions.assertNull(resourceMethod.call(context, builder));
    }

    interface CallableResourceMethods {
        @GET
        String get();

        @POST
        void getVoid();

        @GET
        List<String> getList();

        @GET
        String getPathParam(@PathParam("param") String value);

        @GET
        String getPathParam(@PathParam("param") int value);

        @GET
        String getPathParam(@PathParam("param") Double value);

        @GET
        String getPathParam(@PathParam("param") short value);

        @GET
        String getPathParam(@PathParam("param") float value);

        @GET
        String getPathParam(@PathParam("param") byte value);

        @GET
        String getPathParam(@PathParam("param") boolean value);

        @GET
        String getPathParam(@PathParam("param") BigDecimal value);

        @GET
        String getQueryParam(@QueryParam("param") String value);

        @GET
        String getQueryParam(@QueryParam("param") int value);

        @GET
        String getQueryParam(@QueryParam("param") Double value);

        @GET
        String getQueryParam(@QueryParam("param") short value);

        @GET
        String getQueryParam(@QueryParam("param") float value);

        @GET
        String getQueryParam(@QueryParam("param") byte value);

        @GET
        String getQueryParam(@QueryParam("param") boolean value);

        @GET
        String getQueryParam(@QueryParam("param") BigDecimal value);
    }
}
