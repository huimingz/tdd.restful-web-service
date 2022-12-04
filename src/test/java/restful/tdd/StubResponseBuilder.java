package restful.tdd;

import jakarta.ws.rs.core.*;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.*;

public class StubResponseBuilder extends Response.ResponseBuilder {
    private Object entity;
    private int status;

    private Set<String> allowed = new HashSet<>();

    @Override
    public Response build() {
        OutboundResponse response = Mockito.mock(OutboundResponse.class);
        Mockito.when(response.getEntity()).thenReturn(this.entity);
        Mockito.when(response.getStatus()).thenReturn(this.status);
        Mockito.when(response.getAllowedMethods()).thenReturn(allowed);
        Mockito.when(response.getGenericEntity()).thenReturn((GenericEntity) entity);
        Mockito.when(response.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        return response;
    }

    @Override
    public Response.ResponseBuilder clone() {
        return this;
    }

    @Override
    public Response.ResponseBuilder status(int status) {
        this.status = status;
        return this;
    }

    @Override
    public Response.ResponseBuilder status(int status, String reasonPhrase) {
        this.status = status;
        return this;
    }

    @Override
    public Response.ResponseBuilder entity(Object entity) {
        this.entity = entity;
        return this;
    }

    @Override
    public Response.ResponseBuilder entity(Object entity, Annotation[] annotations) {
        return this;
    }

    @Override
    public Response.ResponseBuilder allow(String... methods) {
        Collections.addAll(allowed, methods);
        return this;
    }

    @Override
    public Response.ResponseBuilder allow(Set<String> methods) {
        allowed.addAll(methods);
        return this;
    }

    @Override
    public Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
        return this;
    }

    @Override
    public Response.ResponseBuilder encoding(String encoding) {
        return this;
    }

    @Override
    public Response.ResponseBuilder header(String name, Object value) {
        return this;
    }

    @Override
    public Response.ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
        return this;
    }

    @Override
    public Response.ResponseBuilder language(String language) {
        return this;
    }

    @Override
    public Response.ResponseBuilder language(Locale language) {
        return this;
    }

    @Override
    public Response.ResponseBuilder type(MediaType type) {
        return this;
    }

    @Override
    public Response.ResponseBuilder type(String type) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variant(Variant variant) {
        return this;
    }

    @Override
    public Response.ResponseBuilder contentLocation(URI location) {
        return this;
    }

    @Override
    public Response.ResponseBuilder cookie(NewCookie... cookies) {
        return this;
    }

    @Override
    public Response.ResponseBuilder expires(Date expires) {
        return this;
    }

    @Override
    public Response.ResponseBuilder lastModified(Date lastModified) {
        return this;
    }

    @Override
    public Response.ResponseBuilder location(URI location) {
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(EntityTag tag) {
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(String tag) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variants(Variant... variants) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variants(List<Variant> variants) {
        return this;
    }

    @Override
    public Response.ResponseBuilder links(Link... links) {
        return this;
    }

    @Override
    public Response.ResponseBuilder link(URI uri, String rel) {
        return this;
    }

    @Override
    public Response.ResponseBuilder link(String uri, String rel) {
        return this;
    }
}
