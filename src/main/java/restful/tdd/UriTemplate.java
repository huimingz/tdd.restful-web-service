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


class UriTemplateString implements UriTemplate {

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

    private static String group(String pattern) {
        return "(" + pattern + ")";
    }

    public UriTemplateString(String template) {
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
            return pattern == null ? DEFAULT_VARIABLE_PATTERN : group(pattern);
        });
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return Optional.empty();

        int count = matcher.groupCount();
        Map<String, String> parameters = new HashMap<>();

        for (int i = 0; i < variables.size(); i++) {
            parameters.put(variables.get(i), matcher.group(variableStartFrom + i));
        }


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
                return parameters;
            }
        });
    }

    @Override
    public int compareTo(MatchResult o) {
        return 0;
    }
}