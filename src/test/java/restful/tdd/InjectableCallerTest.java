package restful.tdd;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.util.List;

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

    record LastCall(String name, List<Object> arguments) {
    }

    record InjectableTpeTestCase(Class<?> type, String string, Object value) {
    }
}
