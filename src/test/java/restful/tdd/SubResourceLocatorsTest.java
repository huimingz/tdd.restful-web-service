package restful.tdd;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Optional;

public class SubResourceLocatorsTest {

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            /hello,              hello,             fully matched with URI
            /topics/1234,        1234,              multiple matched choices   
            """)
    public void should_match_path_with_uri(String path, String message, String context) {
        StubUriInfoBuilder builder = new StubUriInfoBuilder();
        builder.addMatchedResult(new Messages());
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());

        Optional<ResourceRouter.ResourceMethod> method = locators.findSubResourceMethods(path, "GET", new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(ResourceContext.class), builder);

        Assertions.assertTrue(method.isPresent());
        Assertions.assertEquals(message, ((Message) builder.getLastMatchedResource()).message);
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(textBlock = """
            /missing,               unmatched resource method
            /hello/content,         unmatched sub-resource method
            """)
    public void should_return_empty_if_not_match_uri(String path, String context) {
        StubUriInfoBuilder builder = new StubUriInfoBuilder();
        builder.addMatchedResult(new Messages());
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());

        Optional<ResourceRouter.ResourceMethod> method = locators.findSubResourceMethods(path, "GET", new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(ResourceContext.class), builder);

        Assertions.assertFalse(method.isPresent());
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
