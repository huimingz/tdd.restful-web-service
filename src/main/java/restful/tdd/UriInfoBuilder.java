package restful.tdd;

interface UriInfoBuilder {
    void pushMatchedPath(String path);

    void addParameter(String name, String value);

    String getUnmatchedPath();
}
