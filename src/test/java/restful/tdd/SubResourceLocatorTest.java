package restful.tdd;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubResourceLocatorTest extends InjectableCallerTest {

    private UriTemplate.MatchResult result;

    @BeforeEach
    public void before() {
        super.before();
        result = Mockito.mock(UriTemplate.MatchResult.class);
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
                    return new Message();
                });
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
    }

    static class Message {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }

    }
}
