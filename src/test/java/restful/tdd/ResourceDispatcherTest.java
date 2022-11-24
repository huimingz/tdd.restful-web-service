package restful.tdd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

public class ResourceDispatcherTest {
    private RuntimeDelegate delegate;
    private Runtime runtime;
    private HttpServletRequest request;
    private ResourceContext context;
    private UriInfoBuilder builder;

    @BeforeEach
    public void before() {
        runtime = Mockito.mock(Runtime.class);

        delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        Mockito.when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());

        request = Mockito.mock(HttpServletRequest.class);
        context = Mockito.mock(ResourceContext.class);
        Mockito.when(request.getServletPath()).thenReturn("/users/1");
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getHeaders(eq(HttpHeaders.ACCEPT))).thenReturn(new Vector<>(List.of(MediaType.WILDCARD)).elements());

        builder = Mockito.mock(UriInfoBuilder.class);
        Mockito.when(runtime.createUriInfoBuilder(same(request))).thenReturn(builder);
    }

    @Test
    public void should_use_matched_root_resource() {
        GenericEntity entity = new GenericEntity("matched", String.class);
        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(
                rootResource(matched("/users/1", result("/1")), returns(entity)),
                rootResource(unmatched("/users/1"))));

        OutboundResponse response = router.dispatch(request, context);

        Assertions.assertSame(entity, response.getEntity());
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void should_sort_matched_root_resource_descending_order() {
        GenericEntity entity1 = new GenericEntity("1", String.class);
        GenericEntity entity2 = new GenericEntity("2", String.class);

        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(
                rootResource(matched("/users/1", result("/1", 2)), returns(entity2)),
                rootResource(matched("/users/1", result("/1", 1)), returns(entity1))));

        OutboundResponse response = router.dispatch(request, context);

        Assertions.assertSame(entity1, response.getEntity());
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void should_return_404_if_no_root_resource_matched() {
        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(rootResource(unmatched("/users/1"))));

        OutboundResponse response = router.dispatch(request, context);

        Assertions.assertNull(response.getEntity());
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void should_return_404_if_no_resource_method_found() {
        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(
                rootResource(matched("/users/1", result("/1", 2)))));

        OutboundResponse response = router.dispatch(request, context);

        Assertions.assertNull(response.getEntity());
        Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void should_return_204_if_method_return_null() {
        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(
                rootResource(matched("/users/1", result("/1", 2)), returns(null))));

        OutboundResponse response = router.dispatch(request, context);

        Assertions.assertNull(response.getEntity());
        Assertions.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    private ResourceRouter.RootResource rootResource(UriTemplate uriTemplate) {
        ResourceRouter.RootResource unmatched = Mockito.mock(ResourceRouter.RootResource.class);
        Mockito.when(unmatched.getUriTemplate()).thenReturn(uriTemplate);
        Mockito.when(unmatched.matche(eq("/1"), eq("GET"), eq(new String[]{MediaType.WILDCARD}), eq(builder))).thenReturn(Optional.empty());
        return unmatched;
    }

    private UriTemplate unmatched(String path) {
        UriTemplate unmatchedUriTemplate = Mockito.mock(UriTemplate.class);
        Mockito.when(unmatchedUriTemplate.match(eq(path))).thenReturn(Optional.empty());
        return unmatchedUriTemplate;
    }

    private ResourceRouter.RootResource rootResource(UriTemplate uriTemplate, ResourceRouter.ResourceMethod method) {
        ResourceRouter.RootResource matched = Mockito.mock(ResourceRouter.RootResource.class);
        Mockito.when(matched.getUriTemplate()).thenReturn(uriTemplate);
        Mockito.when(matched.matche(eq("/1"), eq("GET"), eq(new String[]{MediaType.WILDCARD}), eq(builder))).thenReturn(Optional.of(method));
        return matched;
    }

    private ResourceRouter.ResourceMethod returns(GenericEntity entity) {
        ResourceRouter.ResourceMethod method = Mockito.mock(ResourceRouter.ResourceMethod.class);
        Mockito.when(method.call(same(context), same(builder))).thenReturn(entity);
        return method;
    }

    private UriTemplate matched(String path, UriTemplate.MatchResult result) {
        UriTemplate matchedUriTemplate = Mockito.mock(UriTemplate.class);
        Mockito.when(matchedUriTemplate.match(eq(path))).thenReturn(Optional.of(result));
        return matchedUriTemplate;
    }

    private static UriTemplate.MatchResult result(String path) {
        return new FakeMatchResult(path, 0);
    }

    private static UriTemplate.MatchResult result(String path, Integer order) {
        return new FakeMatchResult(path, order);
    }

    static class FakeMatchResult implements UriTemplate.MatchResult {
        private String remaining;
        private Integer order;

        public FakeMatchResult(String remaining, Integer order) {
            this.remaining = remaining;
            this.order = order;
        }

        @Override
        public String getMatchedPath() {
            return null;
        }

        @Override
        public String getRemaining() {
            return remaining;
        }

        @Override
        public Map<String, String> getMatchedPathParameters() {
            return null;
        }

        @Override
        public int compareTo(UriTemplate.MatchResult o) {
            return this.order.compareTo(((FakeMatchResult) o).order);
        }
    }
}
