package restful.tdd;

import java.util.Map;
import java.util.Optional;

interface UriTemplate extends Comparable<UriTemplate.MatchResult> {
    interface MatchResult extends Comparable {
        String getMatchedPath();

        String getRemaining();

        Map<String, String> getMatchedPathParameters();
    }

    Optional<MatchResult> match(String path);
}
