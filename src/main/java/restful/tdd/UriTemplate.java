package restful.tdd;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface UriTemplate extends Comparable<UriTemplate.MatchResult> {
    Optional<MatchResult> match(String path);

    interface MatchResult extends Comparable<MatchResult> {
        String getMatchedPath();

        String getRemaining();

        Map<String, String> getMatchedPathParameters();
    }
}



class UriTemplateString implements UriTemplate {

    private final Pattern pattern;
    private final Pattern variable = Pattern.compile("\\{\\w[\\w\\.-]*\\}");;

    public UriTemplateString(String template) {
        String templateWithVariable = variable(template);
        pattern = Pattern.compile("(" + templateWithVariable + ")" + "(/.*)?");
    }

    private String variable(String template) {
        return variable.matcher(template).replaceAll("([^/]+?)");
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int count = matcher.groupCount();

        return Optional.of(new MatchResult() {

            @Override
            public int compareTo(MatchResult o) {
                return 0;
            }

            @Override
            public String getMatchedPath() {
                return matcher.group(1);
            }

            @Override
            public String getRemaining() {
                return matcher.group(count);
            }

            @Override
            public Map<String, String> getMatchedPathParameters() {
                return null;
            }
        });
    }

    @Override
    public int compareTo(MatchResult o) {
        return 0;
    }
}