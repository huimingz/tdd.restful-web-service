package restful.tdd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
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

    private Runtime runtime;
    private List<ResourceRouter.RootResource> rootResources;

    public DefaultResourceRouter(Runtime runtime, List<RootResource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();
        UriInfoBuilder uri = runtime.createUriInfoBuilder(request);

        Optional<ResourceMethod> method = rootResources.stream()
                .map(resource -> match(path, resource))
                .filter(Result::isMatched)
                .sorted()
                .findFirst()
                .flatMap(result -> result.findResourceMethod(request, uri));

        if (method.isEmpty()) {
            return (OutboundResponse) Response.status(Response.Status.NOT_FOUND).build();
        }

        return (OutboundResponse) method.map(m -> m.call(resourceContext, uri)).map(entity -> Response.ok(entity).build()).orElseGet(() -> Response.noContent().build());
    }

    private Result match(String path, RootResource resource) {
        return new Result(resource.getUriTemplate().match(path), resource);
    }


    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) implements Comparable<Result> {
        @Override
        public int compareTo(Result o) {
            // return this.matched.get().compareTo(o.matched.get());
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }

        private boolean isMatched() {
            return matched.isPresent();
        }

        private Optional<ResourceMethod> findResourceMethod(HttpServletRequest request, UriInfoBuilder uri) {
            return resource.matche(matched.get().getRemaining(), request.getMethod(), Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uri);
        }
    }
}


class RootResourceClass implements ResourceRouter.RootResource {

    private Class<?> usersClass;

    public RootResourceClass(Class<?> usersClass) {

        this.usersClass = usersClass;
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> matche(String path, String method, String[] mediaTypes, UriInfoBuilder builder) {
        return Optional.empty();
    }

    @Override
    public UriTemplate getUriTemplate() {
        return new UriTemplate() {
            @Override
            public Optional<MatchResult> match(String path) {
                return Optional.empty();
            }

            @Override
            public int compareTo(MatchResult o) {
                return 0;
            }
        };
    }
}