package restful.tdd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.regex.Pattern;

public class ResourceDispatcherTest {
    @Test
    public void should() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ResourceContext context = Mockito.mock(ResourceContext.class);

        Mockito.when(request.getServletPath()).thenReturn("/users");

        Router router = new Router(Users.class);

        OutboundResponse response = router.dispatch(request, context);
        GenericEntity<String> entity = (GenericEntity<String>) response.getEntity();
        Assertions.assertEquals("all", entity.getEntity());
    }

    static class Router implements ResourceRouter {
        private Map<Pattern, Class<?>> routerTable;

        public Router(Class<Users> rootResource) {
            Path path = rootResource.getAnnotation(Path.class);
            routerTable.put(Pattern.compile(path.value() + "(/.*)?"), rootResource);

        }

        @Override
        public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
            String path = request.getServletPath();
            Pattern matched = routerTable.keySet().stream().filter(p -> p.matcher(path).matches()).findFirst().get();
            Class<?> resource = routerTable.get(matched);

//            resource

            return null;
        }
    }

    @Path("/users")
    static class Users {
        @GET
        public String get() {
            return "all";
        }
    }
}
