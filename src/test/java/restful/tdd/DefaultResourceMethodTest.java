package restful.tdd;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;

public class DefaultResourceMethodTest extends InjectableCallerTest {

    @Override
    protected Object initResource() {
        return Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{CallableResourceMethods.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    lastCall = new LastCall(
                            getMethodName(name, Arrays.stream(method.getParameters()).map(p -> p.getType()).toList()),
                            args != null ? List.of(args) : List.of());
                    return "getList".equals(method.getName()) ? new ArrayList<String>() : null;
                });
    }

    private static String getMethodName(String name, List<? extends Class<?>> classStream) {
        return name + "(" + classStream.stream().map(t -> t.getSimpleName()).collect(Collectors.joining(",")) + ")";
    }

    @Test
    public void should_call_resource_method() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("get");
        resourceMethod.call(context, builder);

        Assertions.assertEquals("get()", lastCall.name());
    }

    private static DefaultResourceMethod getResourceMethod(String methodName, Class... types) throws NoSuchMethodException {
        return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(methodName, types));
    }

    @TestFactory
    public List<DynamicTest> inject_convertable_types() {
        List<DynamicTest> tests = new ArrayList<>();

        List<InjectableTpeTestCase> typeCases = List.of(
                new InjectableTpeTestCase(String.class, "string", "string"),
                new InjectableTpeTestCase(int.class, "1", 1),
                new InjectableTpeTestCase(Double.class, "3.14", 3.14),
                new InjectableTpeTestCase(float.class, "3.14", 3.14f),
                new InjectableTpeTestCase(short.class, "323", (short) 323),
                new InjectableTpeTestCase(byte.class, "42", (byte) 42),
                new InjectableTpeTestCase(boolean.class, "true", true),
                new InjectableTpeTestCase(Converter.class, "Factory", Converter.Factory),
                new InjectableTpeTestCase(BigDecimal.class, "314", new BigDecimal("314"))
        );

        List<String> paramTypes = List.of("getQueryParam", "getPathParam");

        for (String type : paramTypes) {
            for (InjectableTpeTestCase testCase : typeCases) {
                tests.add(DynamicTest.dynamicTest("should inject " + testCase.type().getSimpleName() + " to " + type, () -> {
                    verifyResourceMethodCalled(type, testCase.type(), testCase.string(), testCase.value());
                }));
            }
        }
        return tests;
    }

    @TestFactory
    public List<DynamicTest> inject_context_object() {
        List<DynamicTest> tests = new ArrayList<>();

        List<InjectableTpeTestCase> typeCases = List.of(
                new InjectableTpeTestCase(SomeServiceInContext.class, "N/A", service),
                new InjectableTpeTestCase(ResourceContext.class, "N/A", context),
                new InjectableTpeTestCase(UriInfo.class, "N/A", uriInfo)
        );

        List<String> paramTypes = List.of("getQueryParam");

        for (InjectableTpeTestCase testCase : typeCases) {
            tests.add(DynamicTest.dynamicTest("should inject " + testCase.type().getSimpleName() + " to getContext", () -> {
                verifyResourceMethodCalled("getContext", testCase.type(), testCase.string(), testCase.value());
            }));
        }
        return tests;
    }


    private void verifyResourceMethodCalled(String method, Class<?> type, String paramValue, Object value) throws NoSuchMethodException {
        parameters.put("param", List.of(paramValue));

        callInjectable(method, type);

        Assertions.assertEquals(getMethodName(method, List.of(type)), lastCall.name());
        Assertions.assertEquals(List.of(value), lastCall.arguments());
    }

    private void callInjectable(String method, Class<?> type) throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod(method, type);
        resourceMethod.call(context, builder);
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
        String getPathParam(@PathParam("param") Converter value);

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

        @GET
        String getQueryParam(@QueryParam("param") Converter value);

        @GET
        String getContext(@Context SomeServiceInContext context);

        @GET
        String getContext(@Context ResourceContext context);

        @GET
        String getContext(@Context UriInfo uriInfo);
    }
}

enum Converter {
    Primitive, Constructor, Factory;
}

interface SomeServiceInContext {
}