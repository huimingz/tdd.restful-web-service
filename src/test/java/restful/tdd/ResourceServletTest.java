package restful.tdd;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.HttpResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class ResourceServletTest extends ServletTest {
    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;

    @Override
    protected Servlet getServlet() {
        runtime = Mockito.mock(Runtime.class);
        router = Mockito.mock(ResourceRouter.class);
        resourceContext = Mockito.mock(ResourceContext.class);

        Mockito.when(runtime.getResourceRouter()).thenReturn(router);
        Mockito.when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);

        return new ResourceServlet(runtime);
    }

    @Test
    public void should_use_status_code_from_response() throws Exception {
        OutboundResponse response = Mockito.mock(OutboundResponse.class);
        Mockito.when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());
        Mockito.when(response.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        Mockito.when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);

        HttpResponse<String> httpResponse = get("/test");

        Assertions.assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_use_http_headers_from_response() throws Exception {
        OutboundResponse response = Mockito.mock(OutboundResponse.class);
        RuntimeDelegate delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        Mockito.when(delegate.createHeaderDelegate(eq(NewCookie.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<NewCookie>() {
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            @Override
            public String toString(NewCookie value) {
                return value.getName() + "=" + value.getValue();
            }
        });

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        NewCookie sessionId = new NewCookie.Builder("SESSION_ID").value("session").build();
        NewCookie userId = new NewCookie.Builder("USER_ID").value("user").build();
        headers.addAll("Set-Cookie", sessionId, userId);
        Mockito.when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());
        Mockito.when(response.getHeaders()).thenReturn(headers);
        Mockito.when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);

        HttpResponse<String> httpResponse = get("/test");

        Assertions.assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    // TODO: writer body using MessageBodyWriter
    // TODO: 500 if MessageBodyWriter not found
    // TODO: throw WebApplicationException with response, use response
    // TODO: throw WebApplicationException with null response, use ExceptionMapper build response
    // TODO: throw other exception, use ExceptionMapper build response
}
