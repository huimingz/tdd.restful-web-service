package restful.tdd;

import java.util.*;
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


class PathTemplate implements UriTemplate {

    private static final String LEFT_BRACKET = "\\{";
    private static final String RIGHT_BRACKET = "}";
    private static final String VARIABLE_BANE = "\\w[\\w\\.-]*";
    private static final String NON_BRACKETS = "[^\\{}]+";
    public static final String DEFAULT_VARIABLE_PATTERN = "([^/]+?)";


    private int variableStartFrom = 2;

    private final Pattern pattern;
    private final Pattern variable = Pattern.compile(LEFT_BRACKET + group(VARIABLE_BANE) + group(":" + group(NON_BRACKETS)) + "?" + RIGHT_BRACKET);
    private static final int VARIABLE_NAME_GROUP = 1;
    private static final int VARIABLE_PATTERN_GROUP = 3;
    private final List<String> variables = new ArrayList<>();
    private int specificPatternCount = 0;

    private static String group(String pattern) {
        return "(" + pattern + ")";
    }

    public PathTemplate(String template) {
        pattern = Pattern.compile(group(variable(template)) + "(/.*)?");
        variableStartFrom = 2;
    }

    private String variable(String template) {
        return variable.matcher(template).replaceAll(result -> {
            String variableName = result.group(VARIABLE_NAME_GROUP);
            String pattern = result.group(VARIABLE_PATTERN_GROUP);

            if (variables.contains(variableName))
                throw new IllegalArgumentException("duplicate variable " + variableName);

            variables.add(variableName);
            if (pattern != null) {
                this.specificPatternCount++;
                return group(pattern);
            }
            return DEFAULT_VARIABLE_PATTERN;
        });
    }


    class PathMatchResult implements MatchResult {

        private final int specificParameterCount;
        private int count;
        private int matchLiteralCount;
        private Matcher matcher;
        private Map<String, String> parameters = new HashMap<>();

        public PathMatchResult(Matcher matcher) {
            this.matcher = matcher;
            this.count = matcher.groupCount();
            this.matchLiteralCount = matcher.group(1).length();
            this.specificParameterCount = specificPatternCount;

            for (int i = 0; i < variables.size(); i++) {
                this.parameters.put(variables.get(i), matcher.group(variableStartFrom + i));
                this.matchLiteralCount -= matcher.group(variableStartFrom + i).length();
            }
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
            return parameters;
        }

        @Override
        public int compareTo(MatchResult o) {
            PathMatchResult result = (PathMatchResult) o;
            if (matchLiteralCount > result.matchLiteralCount) return -1;
            if (matchLiteralCount < result.matchLiteralCount) return 1;
            if (parameters.size() > result.parameters.size()) return -1;
            if (parameters.size() < result.parameters.size()) return 1;
            if (specificParameterCount > result.specificParameterCount) return -1;
            if (specificParameterCount < result.specificParameterCount) return 1;
            return 0;
        }
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return Optional.empty();

        return Optional.of(new PathMatchResult(matcher));
    }

    @Override
    public int compareTo(MatchResult o) {
        return 0;
    }
}