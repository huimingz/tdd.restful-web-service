package restful.tdd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

public class ResourceDispatcherTest {
    private RuntimeDelegate delegate;
    private Runtime runtime;
    private HttpServletRequest request;
    private ResourceContext context;

    @BeforeEach
    public void before() {
        runtime = Mockito.mock(Runtime.class);

        delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        Mockito.when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());

        request = Mockito.mock(HttpServletRequest.class);
        context = Mockito.mock(ResourceContext.class);
        Mockito.when(request.getServletPath()).thenReturn("/users");
    }

    @Test
    public void should_use_matched_root_resource() {
        ResourceRouter.RootResource matched = Mockito.mock(ResourceRouter.RootResource.class);
        UriTemplate matchedUriTemplate = Mockito.mock(UriTemplate.class);
        UriTemplate.MatchResult result = Mockito.mock(UriTemplate.MatchResult.class);
        Mockito.when(matched.getUriTemplate()).thenReturn(matchedUriTemplate);
        Mockito.when(matchedUriTemplate.match(any())).thenReturn(Optional.of(result));
        ResourceRouter.ResourceMethod method = Mockito.mock(ResourceRouter.ResourceMethod.class);
        Mockito.when(matched.matche(any(), any(), any(), any())).thenReturn(Optional.of(method));
        GenericEntity entity = new GenericEntity("matched", String.class);
        Mockito.when(method.call(any(), any())).thenReturn(entity);

        ResourceRouter.RootResource unmatched = Mockito.mock(ResourceRouter.RootResource.class);
        UriTemplate unmatchedUriTemplate = Mockito.mock(UriTemplate.class);
        Mockito.when(matched.getUriTemplate()).thenReturn(unmatchedUriTemplate);
        Mockito.when(unmatchedUriTemplate.match(any())).thenReturn(Optional.empty());

        DefaultResourceRouter router = new DefaultResourceRouter(List.of(matched, unmatched));
        OutboundResponse response = router.dispatch(request, context);

        Assertions.assertSame(entity, response.getGenericEntity());
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
