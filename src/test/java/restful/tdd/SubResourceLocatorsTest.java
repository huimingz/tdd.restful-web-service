package restful.tdd;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class SubResourceLocatorsTest {

    @Test
    public void should_match_path_with_uri() {
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());

        ResourceRouter.SubResourceLocator locator = locators.findSubResource("/hello").get();

        Assertions.assertEquals("Messages.hello", locator.toString());
    }

    @Test
    public void should_return_empty_if_not_match_uri() {
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());

        Optional<ResourceRouter.SubResourceLocator> locator = locators.findSubResource("/missing");

        Assertions.assertTrue(locator.isEmpty());
    }

    @Test
    public void should_call_locator_method_to_generate_sub_resource() {
        UriTemplate.MatchResult result = Mockito.mock(UriTemplate.MatchResult.class);
        Mockito.when(result.getRemaining()).thenReturn(null);
        UriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();
        uriInfoBuilder.addMatchedResult(new Messages());

        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        ResourceRouter.SubResourceLocator subResourceLocator = locators.findSubResource("/hello").get();

        ResourceRouter.Resource subResource = subResourceLocator.getSubResource(Mockito.mock(ResourceContext.class), uriInfoBuilder);
        ResourceRouter.ResourceMethod method = subResource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, uriInfoBuilder).get();

        Assertions.assertEquals("Message.content", method.toString());
        Assertions.assertEquals("hello", (( Message) uriInfoBuilder.getLastMatchedResource()).message);
    }

    @Path("/messages")
    static class Messages {

        @Path("/hello")
        public Message hello() {
            return new Message("hello");
        }

        @Path("/topics/{id}")
        public Message id() {
            return new Message("id");
        }

        @Path("/topics/1234")
        public Message message1234() {
            return new Message("1234");
        }
    }

    static class Message {
        private String message;

        public Message(String message) {
            this.message = message;
        }

        @GET
        public String content() {
            return "content";
        }
    }

}
