package restful.tdd;

interface UriInfoBuilder {
    Object getLastMatchedResource();

    void addMatchedResult(Object resource);
}
