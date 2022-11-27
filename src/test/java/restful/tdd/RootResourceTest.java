package restful.tdd;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Optional;

public class RootResourceTest {

    @Test
    public void should_get_uri_template_from_path_annotation() {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate template = resource.getUriTemplate();

        Assertions.assertTrue(template.match("/messages/hello").isPresent());
    }

    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            GET,        /messages/hello,            Messages.hello,                 GET and URI match
            GET,        /messages/ah,               Messages.ah,                    GET and URI match
            POST,       /messages/hello,            Messages.postHello,             POST and URI match
            GET,        /messages/topics/1234,      Messages.topic1234,             GET with multiply choices
            GET,        /messages,                  Messages.get,                   GET with resource method without Path
            PUT,        /messages/hello,            Messages.putHello,              PUT and URI match
            PATCH,      /messages/hello,            Messages.patchHello,            PATCH and URI match
            DELETE,     /messages/hello,            Messages.deleteHello,           DELETE and URI match
            HEAD,       /messages/hello,            Messages.headHello,             HEAD and URI match
            OPTIONS,    /messages/hello,            Messages.optionsHello,          OPTIONS and URI match
            """
    )
    public void should_match_resource_method(String httpMethod, String path, String resourceMethod, String context) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);

        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();

        ResourceRouter.ResourceMethod method = resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(UriInfoBuilder.class)).get();

        Assertions.assertEquals(resourceMethod, method.toString());
    }

    // TODO: if sub resource locator matches uri, using it to do follow up matching.

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,        /missing-messages/1,             URI not match
            POST,       /missing-messages,               Http method not matched
            """)
    public void should_return_empty_if_not_not_match(String httpMethod, String path, String context) {
        ResourceRouter.RootResource resource = new RootResourceClass(MissingMessages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        Optional<ResourceRouter.ResourceMethod> method = resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(UriInfoBuilder.class));

        Assertions.assertTrue(method.isEmpty());
    }


    // TODO: if no method / sub resource locator matches, return 404.
    // TODO: if resource class does not have a path annotation, throw illegal argument.
    // TODO: Head and Options special case.

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
        @Path("/ah")
        @Produces(MediaType.TEXT_PLAIN)
        public String ah() {
            return "ah";
        }

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
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
