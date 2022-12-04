package restful.tdd;

import jakarta.ws.rs.core.UriInfo;

import java.util.Map;

interface UriInfoBuilder {
    Object getLastMatchedResource();

    void addMatchedResult(Object resource);

    void addMatchedPathParameters(Map<String, String> pathParameters);

    UriInfo createUriInfo();
}
