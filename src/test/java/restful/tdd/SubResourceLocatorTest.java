package restful.tdd;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SubResourceLocatorTest extends InjectableCallerTest {

    private UriTemplate.MatchResult result;
    Map<String, String> matchedPathParameters = Map.of("param", "param");

    @BeforeEach
    public void before() {
        super.before();
        result = Mockito.mock(UriTemplate.MatchResult.class);
        Mockito.when(result.getMatchedPathParameters()).thenReturn(matchedPathParameters);
    }

    @Override
    protected Object initResource() {
        return Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{SubResourceMethods.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    lastCall = new DefaultResourceMethodTest.LastCall(
                            getMethodName(name, Arrays.stream(method.getParameters()).map(p -> p.getType()).toList()),
                            args != null ? List.of(args) : List.of());
                    if (method.getName().equals("throwWebApplicationException")) {
                        throw new WebApplicationException(300);
                    }
                    return new Message();
                });
    }

    @Override
    public List<DynamicTest> inject_convertable_types() {
        return null;
    }

    @Override
    public List<DynamicTest> inject_context_object() {
        return null;
    }

    @Test
    public void should_add_matched_path_parameter_to_builder() throws NoSuchMethodException {
        parameters.put("param", List.of("param"));

        callInjectable(String.class, "getPathParam");

        Mockito.verify(builder).addMatchedPathParameters(matchedPathParameters);
    }

    protected static String getMethodName(String name, List<? extends Class<?>> classStream) {
        return name + "(" + classStream.stream().map(t -> t.getSimpleName()).collect(Collectors.joining(",")) + ")";
    }

    @Test
    public void should_inject_path_parameter_to_sub_resource_method() throws NoSuchMethodException {
        Class<?> type = String.class;
        String methodName = "getPathParam";
        String paramString = "path";
        String paramValue = "path";

        parameters.put("param", List.of(paramString));

        callInjectable(type, methodName);

        Assertions.assertEquals(getMethodName(methodName, List.of(type)), lastCall.name());
        Assertions.assertEquals(List.of(paramValue), lastCall.arguments());
    }

    @Override
    protected void callInjectable(Class<?> type, String methodName) throws NoSuchMethodException {
        SubResourceLocators.SubResourceLocator locator = new SubResourceLocators.SubResourceLocator(SubResourceMethods.class.getMethod(methodName, type));
        locator.match(result, "GET", new String[]{}, context, builder);
    }

    interface SubResourceMethods {
        @Path("/messages/{param}")
        Message getPathParam(@PathParam("param") String path);

        @Path("/message/{param}")
        Message throwWebApplicationException(@PathParam("param") String path);
    }

    static class Message {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }
    }
}
