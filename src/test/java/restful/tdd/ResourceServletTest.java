package restful.tdd;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.*;

public class ResourceServletTest extends ServletTest {
    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;
    private Providers providers;
    private OutboundResponseBuilder response;

    @Override
    protected Servlet getServlet() {
        runtime = Mockito.mock(Runtime.class);
        router = Mockito.mock(ResourceRouter.class);
        resourceContext = Mockito.mock(ResourceContext.class);
        providers = Mockito.mock(Providers.class);

        Mockito.when(runtime.getResourceRouter()).thenReturn(router);
        Mockito.when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        Mockito.when(runtime.getProviders()).thenReturn(providers);

        return new ResourceServlet(runtime);
    }

    @BeforeEach
    public void before() {
        response = new OutboundResponseBuilder();

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
    }

    @Test
    public void should_use_status_code_from_response() throws Exception {
        response.status(Response.Status.NOT_MODIFIED).returnFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        Assertions.assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_use_http_headers_from_response() throws Exception {
        response.status(Response.Status.NOT_MODIFIED).headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build()).returnFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        Assertions.assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    // TODO: writer body using MessageBodyWriter

    @Test
    public void should_write_entity_to_http_response_using_message_body_writer() throws Exception {
        GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        response.entity(entity, annotations).mediaType(mediaType).returnFrom(router);

        Mockito.when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), eq(annotations), eq(mediaType))).thenReturn(new MessageBodyWriter<String>() {
            @Override
            public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                return false;
            }

            @Override
            public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                PrintWriter writer = new PrintWriter(entityStream);
                writer.write(s);
                writer.flush();
            }
        });

        HttpResponse<String> httpResponse = get("/test");
        Assertions.assertEquals("entity", httpResponse.body());
    }

    @Test
    public void should_use_response_from_web_application_exception() throws Exception {
        response.status(Response.Status.FORBIDDEN)
                .headers(HttpHeaders.SET_COOKIE, new NewCookie.Builder("SESSION_ID").value("session").build())
                .entity(new GenericEntity<>("error", String.class), new Annotation[0])
                .throwFrom(router);

        HttpResponse<String> httpResponse = get("/test");

        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
        Assertions.assertArrayEquals(new String[]{"SESSION_ID=session"}, httpResponse.headers().allValues(HttpHeaders.SET_COOKIE).toArray(String[]::new));
        Assertions.assertEquals("error", httpResponse.body());
    }

    // TODO: throw other exception, use ExceptionMapper build response
    @Test
    public void should_build_response_by_exception_mapper_if_null_response_from_web_application_exception() throws Exception {
        Mockito.when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        Mockito.when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(new ExceptionMapper<RuntimeException>() {
            @Override
            public Response toResponse(RuntimeException exception) {
                return response.status(Response.Status.FORBIDDEN).build();
            }
        });

        HttpResponse<String> httpResponse = get("/test");

        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    // TODO: 500 if MessageBodyWriter not found
    // TODO: entity is null, ignore MessageBodyWriter

    private void response(Response.Status status, MultivaluedMap<String, Object> headers, GenericEntity<Object> entity, Annotation[] annotations, MediaType mediaType) {
        OutboundResponse response = Mockito.mock(OutboundResponse.class);
        Mockito.when(response.getStatus()).thenReturn(status.getStatusCode());
        Mockito.when(response.getHeaders()).thenReturn(headers);
        Mockito.when(response.getGenericEntity()).thenReturn(entity);
        Mockito.when(response.getAnnotations()).thenReturn(annotations);
        Mockito.when(response.getMediaType()).thenReturn(mediaType);
        Mockito.when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
    }

    class OutboundResponseBuilder {
        private Response.Status status = Response.Status.OK;
        private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        private GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        private Annotation[] annotations = new Annotation[0];
        private MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public OutboundResponseBuilder headers(String name, Object... values) {
            this.headers.addAll(name, values);
            return this;
        }

        public OutboundResponseBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        public OutboundResponseBuilder entity(GenericEntity<Object> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        public OutboundResponseBuilder mediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        void returnFrom(ResourceRouter router) {
            build(response -> Mockito.when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));
        }

        void throwFrom(ResourceRouter router) {
            build(response -> {
                WebApplicationException exception = new WebApplicationException(response);
                Mockito.when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
            });
        }

        void build(Consumer<OutboundResponse> consumer) {
            OutboundResponse response = build();
            consumer.accept(response);
        }

        OutboundResponse build() {
            OutboundResponse response = Mockito.mock(OutboundResponse.class);
            Mockito.when(response.getStatus()).thenReturn(this.status.getStatusCode());
            Mockito.when(response.getHeaders()).thenReturn(this.headers);
            Mockito.when(response.getStatusInfo()).thenReturn(this.status);
            Mockito.when(response.getGenericEntity()).thenReturn(this.entity);
            Mockito.when(response.getAnnotations()).thenReturn(this.annotations);
            Mockito.when(response.getMediaType()).thenReturn(this.mediaType);

            stubMessageBodyWriter();
            return response;
        }

        private void stubMessageBodyWriter() {
            Mockito.when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), same(annotations), eq(mediaType))).thenReturn(new MessageBodyWriter<>() {
                @Override
                public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                    return false;
                }

                @Override
                public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                    PrintWriter writer = new PrintWriter(entityStream);
                    writer.write(s);
                    writer.flush();
                }
            });
        }

    }
}
