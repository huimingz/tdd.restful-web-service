package restful.tdd;

import jakarta.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;

class StubUriInfoBuilder implements UriInfoBuilder {
    private List<Object> matchedResult = new ArrayList<>();
    private UriInfo uriInfo;

    public StubUriInfoBuilder() {
    }

    public StubUriInfoBuilder(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Override
    public Object getLastMatchedResource() {
        return this.matchedResult.get(this.matchedResult.size() - 1);
    }

    @Override
    public void addMatchedResult(Object resource) {
        this.matchedResult.add(resource);
    }

    @Override
    public UriInfo createUriInfo() {
        return uriInfo;
    }
}
