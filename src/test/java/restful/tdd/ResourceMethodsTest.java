package restful.tdd;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

public class ResourceMethodsTest {

    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            GET,        /messages/hello,            Messages.hello,                 GET and URI match
            POST,       /messages/hello,            Messages.postHello,             POST and URI match
            GET,        /messages/topics/1234,      Messages.topic1234,             GET with multiply choices
            GET,        /messages,                  Messages.get,                   GET with resource method without Path
            PUT,        /messages/hello,            Messages.putHello,              PUT and URI match
            PATCH,      /messages/hello,            Messages.patchHello,            PATCH and URI match
            DELETE,     /messages/hello,            Messages.deleteHello,           DELETE and URI match
            HEAD,       /messages/hello,            Messages.headHello,             HEAD and URI match
            OPTIONS,    /messages/hello,            Messages.optionsHello,          OPTIONS and URI match
            HEAD,       /messages/head,             Messages.getHead,               HEAD with GET resource method
            """
    )
    public void should_match_resource_method_in_root_resource(String httpMethod, String path, String resourceMethod, String context) {
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match(path).get();
        String remaining = result.getRemaining() != null ? result.getRemaining(): "";

        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethods(remaining, httpMethod).get();

        Assertions.assertEquals(resourceMethod, method.toString());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,        /missing-messages/1,             URI not match
            POST,       /missing-messages,               Http method not matched
            """)
    public void should_return_empty_if_not_not_match(String httpMethod, String path, String context) {
        ResourceMethods resourceMethods = new ResourceMethods(MissingMessages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/missing-messages").match(path).get();
        String remaining = result.getRemaining() != null ? result.getRemaining(): "";

        Optional<ResourceRouter.ResourceMethod> method = resourceMethods.findResourceMethods(remaining, httpMethod);

        Assertions.assertTrue(method.isEmpty());
    }

    @Test
    public void should_convert_get_resource_method_to_head_resource_method() {
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/head").get();

        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethods(result.getRemaining(), "HEAD").get();

        Assertions.assertInstanceOf(HeadResourceMethod.class, method);
    }

    @Test
    public void should_get_options_for_given_uri() {
        RuntimeDelegate delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        Mockito.when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        ResourceContext context = Mockito.mock(ResourceContext.class);
        UriInfoBuilder builder = Mockito.mock(UriInfoBuilder.class);

        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/head").get();

        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethods(result.getRemaining(), "OPTIONS").get();
        Response response = (Response) method.call(context, builder).getEntity();

        Assertions.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        Assertions.assertEquals(Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS), response.getAllowedMethods());
    }

    @Test
    public void should_not_include_head_in_options_if_given_uri_not_have_get_method() {
        RuntimeDelegate delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        Mockito.when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        ResourceContext context = Mockito.mock(ResourceContext.class);
        UriInfoBuilder builder = Mockito.mock(UriInfoBuilder.class);

        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/no-head").get();

        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethods(result.getRemaining(), "OPTIONS").get();
        Response response = (Response) method.call(context, builder).getEntity();

        Assertions.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        Assertions.assertEquals(Set.of(HttpMethod.POST, HttpMethod.OPTIONS), response.getAllowedMethods());
    }

    @Path("/missing-messages")
    static class MissingMessages {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }
    }

    @Path("/messages")
    static class Messages {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "get";
        }

        @GET
        @Path("/head")
        public String getHead() {
            return "head";
        }


        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }

        @POST
        @Path("/no-head")
        @Produces(MediaType.TEXT_PLAIN)
        public void PostNoHead() {
        }

        @POST
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String postHello() {
            return "hello";
        }

        @DELETE
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String deleteHello() {
            return "hello";
        }

        @PUT
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String putHello() {
            return "hello";
        }

        @PATCH
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String patchHello() {
            return "hello";
        }

        @HEAD
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String headHello() {
            return "hello";
        }

        @OPTIONS
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String optionsHello() {
            return "hello";
        }

        @GET
        @Path("/topics/{id}")
        @Produces(MediaType.TEXT_PLAIN)
        public String topicId() {
            return "topicId";
        }

        @GET
        @Path("/topics/1234")
        @Produces(MediaType.TEXT_PLAIN)
        public String topic1234() {
            return "topic1234";
        }

    }
}
