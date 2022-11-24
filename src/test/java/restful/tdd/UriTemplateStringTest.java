package restful.tdd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class UriTemplateStringTest {

    @Test
    public void should_return_empty_if_path_not_matched() {
        UriTemplateString template = new UriTemplateString("/users/1");

        Assertions.assertTrue(template.match("/orders").isEmpty());
    }

    @Test
    public void should_return_matched_result_if_path_matched() {
        UriTemplateString template = new UriTemplateString("/users");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        Assertions.assertEquals("/users", result.getMatchedPath());
        Assertions.assertEquals("/1", result.getRemaining());
        Assertions.assertTrue(result.getMatchedPathParameters().isEmpty());
    }

    @Test
    public void should_return_match_result_if_path_match_with_variable() {
        UriTemplateString template = new UriTemplateString("/users/{id}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        Assertions.assertEquals("/users/1", result.getMatchedPath());
        Assertions.assertNull(result.getRemaining());
        Assertions.assertFalse(result.getMatchedPathParameters().isEmpty());
        Assertions.assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    public void should_return_empty_if_not_match_given_pattern() {
        UriTemplateString template = new UriTemplateString("/users/{id:[0-9]+}");

        Assertions.assertTrue(template.match("/users/id").isEmpty());
    }

    @Test
    public void should_extract_variable_value_by_given_pattern() {
        UriTemplateString template = new UriTemplateString("/users/{id:[0-9]+}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        Assertions.assertEquals("/users/1", result.getMatchedPath());
        Assertions.assertNull(result.getRemaining());
        Assertions.assertFalse(result.getMatchedPathParameters().isEmpty());
        Assertions.assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    public void should_throw_illegal_argument_exception_if_variable_redefined() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new UriTemplateString(("/users/{id:[0-9]+}/{id}")));
    }

    // TODO: throw exception if variable redefined.
    // TODO: comparing result, with match literal, variables, and specified variables.
}
