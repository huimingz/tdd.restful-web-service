package restful.tdd;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface UriHandler {
    UriTemplate getUriTemplate();
}

class UriHandlers {

    // T, K, V, E, R, ?
    public static <T extends UriHandler, R> Optional<R> match(String path, List<T> handlers, Function<UriTemplate.MatchResult, Boolean> matchFunction, Function<Optional<Result<T>>, Optional<R>> mapper) {
        return mapper.apply(handlers.stream()
                .map(m -> new Result<>(m.getUriTemplate().match(path), m, matchFunction))
                .filter(Result::isMatched)
                .sorted()
                .findFirst());
    }

    public static <T extends UriHandler> Optional<T> match(String path, List<T> handlers, Function<UriTemplate.MatchResult, Boolean> matchFunction) {
        return match(path, handlers, matchFunction, r -> r.map(Result::handler));
    }

    public static <T extends UriHandler> Optional<T> match(String path, List<T> handlers) {
        return match(path, handlers, r -> true);
    }

    static record Result<T extends UriHandler>(
            Optional<UriTemplate.MatchResult> matched,
            T handler,
            Function<UriTemplate.MatchResult, Boolean> matchFunction) implements Comparable<Result<T>> {

        public boolean isMatched() {
            return matched.map(matchFunction::apply).orElse(false);
        }

        @Override
        public int compareTo(Result<T> o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }
    }
}
