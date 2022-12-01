package restful.tdd;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DefaultResourceMethodTest {

    @Test
    public void should_call_resource_method() throws NoSuchMethodException {
        CallableResourceMethods resource = Mockito.mock(CallableResourceMethods.class);
        ResourceContext context = Mockito.mock(ResourceContext.class);
        UriInfoBuilder builder = Mockito.mock(UriInfoBuilder.class);
        Mockito.when(builder.getLastMatchedResource()).thenReturn(resource);
        Mockito.when(resource.get()).thenReturn("resource called");

        DefaultResourceMethod resourceMethod = new DefaultResourceMethod(CallableResourceMethods.class.getMethod("get"));

        Assertions.assertEquals(new GenericEntity<>("resource called", String.class), resourceMethod.call(context, builder));
    }

    // TODO: return type

    interface CallableResourceMethods {
        @GET
        String get();
    }
}
