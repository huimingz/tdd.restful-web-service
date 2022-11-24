package restful.tdd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class UriTemplateStringTest {

    @Test
    public void should_return_empty_if_path_not_matched() {
        UriTemplateString template = new UriTemplateString("/users/1");

        Optional<UriTemplate.MatchResult> result = template.match("/orders");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void should_return_matched_result_if_path_matched() {
        UriTemplateString template = new UriTemplateString("/users");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        Assertions.assertEquals("/users", result.getMatchedPath());
        Assertions.assertEquals("/1", result.getRemaining());
    }

    // TODO: path match with variables.
    @Test
    public void should_return_match_result_if_path_match_with_variable() {
        UriTemplateString template = new UriTemplateString("/users/{id}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        Assertions.assertEquals("/users/1", result.getMatchedPath());
        Assertions.assertNull(result.getRemaining());
    }

    // TODO: path match with variables with specific pattern.
    // TODO: throw exception if variable redefined.
    // TODO: comparing result, with match literal, variables, and specified variables.
}
