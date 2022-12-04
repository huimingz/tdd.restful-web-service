package restful.tdd;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;

public abstract class InjectableCallerTest {
    protected ResourceContext context;
    protected UriInfoBuilder builder;
    protected UriInfo uriInfo;
    protected MultivaluedHashMap<String, String> parameters;
    protected DefaultResourceMethodTest.LastCall lastCall;
    protected SomeServiceInContext service;
    private Object resource;

    @BeforeEach
    public void before() {
        resource = initResource();

        context = Mockito.mock(ResourceContext.class);
        uriInfo = Mockito.mock(UriInfo.class);
        builder = Mockito.mock(UriInfoBuilder.class);
        service = Mockito.mock(SomeServiceInContext.class);
        parameters = new MultivaluedHashMap<>();

        Mockito.when(builder.getLastMatchedResource()).thenReturn(resource);
        Mockito.when(builder.createUriInfo()).thenReturn(uriInfo);
        Mockito.when(uriInfo.getPathParameters()).thenReturn(parameters);
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(parameters);
        Mockito.when(context.getResource(eq(SomeServiceInContext.class))).thenReturn(service);
    }

    protected abstract Object initResource();

    protected static String getMethodName(String name, List<? extends Class<?>> classStream) {
        return name + "(" + classStream.stream().map(t -> t.getSimpleName()).collect(Collectors.joining(",")) + ")";
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

        callInjectable(type, method);

        Assertions.assertEquals(InjectableCallerTest.getMethodName(method, List.of(type)), lastCall.name());
        Assertions.assertEquals(List.of(value), lastCall.arguments());
    }

    protected abstract void callInjectable(Class<?> type, String methodName) throws NoSuchMethodException;

    record LastCall(String name, List<Object> arguments) {
    }

    record InjectableTpeTestCase(Class<?> type, String string, Object value) {
    }
}
