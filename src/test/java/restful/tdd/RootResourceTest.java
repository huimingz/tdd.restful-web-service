package restful.tdd;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;

public class RootResourceTest {

    private ResourceContext resourceContext;
    private Messages rootResource;

    @BeforeEach
    public void before() {
        resourceContext = Mockito.mock(ResourceContext.class);
        rootResource = new Messages();
        Mockito.when(resourceContext.getResource(eq(Messages.class))).thenReturn(rootResource);
    }

    @Test
    public void should_get_uri_template_from_path_annotation() {
        ResourceRouter.Resource resource = new RootResourceHandler(Messages.class);
        UriTemplate template = resource.getUriTemplate();

        Assertions.assertTrue(template.match("/messages/hello").isPresent());
    }

    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            GET,        /messages,                      Messages.get,               Map to resource method
            GET,        /messages/1/content,            Message.content,            Map to sub-resource method
            GET,        /messages/1/body,               MessageBody.get,            Map to sub-sub -resource method
            """
    )
    public void should_match_resource_method_in_root_resource(String httpMethod, String path, String resourceMethod, String context) {
        ResourceRouter.Resource resource = new RootResourceHandler(Messages.class);
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();

        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        ResourceRouter.ResourceMethod method = resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, resourceContext, uriInfoBuilder).get();

        Assertions.assertEquals(resourceMethod, method.toString());
    }

    @Test
    public void should_match_resource_method_in_sub_resource() {
        ResourceRouter.Resource resource = new RootResourceHandler(new Message(), Mockito.mock(UriTemplate.class));
        UriTemplate.MatchResult result = Mockito.mock(UriTemplate.MatchResult.class);
        Mockito.when(result.getRemaining()).thenReturn("/content");

        Assertions.assertTrue(resource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, resourceContext, Mockito.mock(UriInfoBuilder.class)).isPresent());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,        /messages/hello,                No matched resource method
            GET,        /messages/1/header,             No matched sub-resource method
            """)
    public void should_return_empty_if_not_not_match_in_root_resource(String httpMethod, String path, String context) {
        ResourceRouter.Resource resource = new RootResourceHandler(Messages.class);
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();

        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        Optional<ResourceRouter.ResourceMethod> method = resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, resourceContext, uriInfoBuilder);

        Assertions.assertTrue(method.isEmpty());
    }

    @Test
    public void should_add_last_match_resource_to_uri_info_builder() {
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();
        RootResourceHandler resource = new RootResourceHandler(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match("/messages").get();

        Optional<ResourceRouter.ResourceMethod> method = resource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, resourceContext, uriInfoBuilder);

        Assertions.assertTrue(uriInfoBuilder.getLastMatchedResource() instanceof Messages);
    }

    // TODO: if resource class does not have a path annotation, throw illegal argument.
    @Test
    public void should_throw_illegal_argument_exception_if_root_resource_not_have_path_annotation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RootResourceHandler(Message.class));
    }

    // TODO: Head and Options special case.


    @Path("/messages")
    static class Messages {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }


        @Path("/{id}")
        public Message getByID() {
            return new Message();
        }
    }

    static class Message {
        @GET
        @Path("/content")
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }

        @Path("/body")
        public MessageBody body() {
            return new MessageBody();
        }
    }

    static class MessageBody {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "body";
        }
    }
}
