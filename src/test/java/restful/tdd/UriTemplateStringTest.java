package restful.tdd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UriTemplateStringTest {

    @Test
    public void should_return_empty_if_path_not_matched() {
        PathTemplate template = new PathTemplate("/users/1");

        Assertions.assertTrue(template.match("/orders").isEmpty());
    }

    @Test
    public void should_return_matched_result_if_path_matched() {
        PathTemplate template = new PathTemplate("/users");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        Assertions.assertEquals("/users", result.getMatchedPath());
        Assertions.assertEquals("/1", result.getRemaining());
        Assertions.assertTrue(result.getMatchedPathParameters().isEmpty());
    }

    @Test
    public void should_return_match_result_if_path_match_with_variable() {
        PathTemplate template = new PathTemplate("/users/{id}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        Assertions.assertEquals("/users/1", result.getMatchedPath());
        Assertions.assertNull(result.getRemaining());
        Assertions.assertFalse(result.getMatchedPathParameters().isEmpty());
        Assertions.assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    public void should_return_empty_if_not_match_given_pattern() {
        PathTemplate template = new PathTemplate("/users/{id:[0-9]+}");

        Assertions.assertTrue(template.match("/users/id").isEmpty());
    }

    @Test
    public void should_extract_variable_value_by_given_pattern() {
        PathTemplate template = new PathTemplate("/users/{id:[0-9]+}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        Assertions.assertEquals("/users/1", result.getMatchedPath());
        Assertions.assertNull(result.getRemaining());
        Assertions.assertFalse(result.getMatchedPathParameters().isEmpty());
        Assertions.assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    public void should_throw_illegal_argument_exception_if_variable_redefined() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PathTemplate(("/users/{id:[0-9]+}/{id}")));
    }

    @Test
    public void should_compare_for_matched_literal() {
        assertSmaller("/users/1234", "/users/1234", "/users/{id}");
    }

    @Test
    public void should_compare_match_variables_if_matched_literal_same() {
        assertSmaller("/users/1234567890/order", "/{resources}/1234567890/{action}", "/users/{id}/order");
    }

    @Test
    public void should_compare_specific_variable_if_matched_literal_variables_same() {
        assertSmaller("/users/1", "/users/{id:[0-9]+}", "/users/{id}");
    }

    @Test
    public void should_compare_equal_match_result() {
        UriTemplate.MatchResult result = new PathTemplate("/users/{id}").match("/users/1").get();

        Assertions.assertEquals(0, result.compareTo(result));
    }


    private static void assertSmaller(String path, String smallerTemplate, String largerTemplate) {
        PathTemplate smaller = new PathTemplate(smallerTemplate);
        PathTemplate larger = new PathTemplate(largerTemplate);

        UriTemplate.MatchResult lhs = smaller.match(path).get();
        UriTemplate.MatchResult rhs = larger.match(path).get();

        Assertions.assertTrue(lhs.compareTo(rhs) < 0);
        Assertions.assertTrue(rhs.compareTo(lhs) > 0);
    }

    // TODO: throw exception if variable redefined.
    // TODO: comparing result, with match literal, variables, and specified variables.
}
