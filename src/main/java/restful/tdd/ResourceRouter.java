package restful.tdd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;

import java.util.List;
import java.util.Optional;

interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> matche(String path, String method, String[] mediaTypes, UriInfoBuilder builder);
    }

    interface RootResource extends Resource {
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod {
        GenericEntity<?> call(ResourceContext context, UriInfoBuilder builder);
    }
}

class DefaultResourceRouter implements ResourceRouter {

    private List<ResourceRouter.RootResource> rootResources;

    public DefaultResourceRouter(List<ResourceRouter.RootResource> rootResources) {
        this.rootResources = rootResources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        return null;
    }
}
