package restful.tdd;

import jakarta.ws.rs.core.UriInfo;

interface UriInfoBuilder {
    Object getLastMatchedResource();

    void addMatchedResult(Object resource);

    UriInfo createUriInfo();
}
