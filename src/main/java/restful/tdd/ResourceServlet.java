package restful.tdd;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;
import java.util.function.Supplier;

public class ResourceServlet extends HttpServlet {
    private final Runtime runtime;
    private final Providers providers;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
        this.providers = runtime.getProviders();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();
        respond(resp, () -> router.dispatch(req, runtime.createResourceContext(req, resp)));
    }

    private void respond(HttpServletResponse resp, Supplier<OutboundResponse> supplier) {
        try {
            respond(resp, supplier.get());
        } catch (WebApplicationException exception) {
            respond(resp, () -> (OutboundResponse) exception.getResponse());
        } catch (Throwable throwable) {
            respond(resp, () -> from(throwable));
        }
    }

    private void respond(HttpServletResponse resp, OutboundResponse response) throws IOException {
        resp.setStatus(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        for (String name : headers.keySet())
            for (Object value : headers.get(name)) {
                RuntimeDelegate.HeaderDelegate headerDelegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
                resp.addHeader(name, headerDelegate.toString(value));
            }
        GenericEntity entity = response.getGenericEntity();
        if (entity != null) {
            MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
            writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(),
                    response.getHeaders(), resp.getOutputStream());
        }
    }

    private OutboundResponse from(Throwable throwable) {
        ExceptionMapper mapper = providers.getExceptionMapper(throwable.getClass());
        return (OutboundResponse) mapper.toResponse(throwable);
    }
}

//public class ResourceServlet extends HttpServlet {
//    private Runtime runtime;
//    private Providers providers;
//
//    public ResourceServlet(Runtime runtime) {
//        this.runtime = runtime;
//        this.providers = runtime.getProviders();
//    }
//
//    @Override
//    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        ResourceRouter router = runtime.getResourceRouter();
//
//        respond(resp, () -> router.dispatch(req, runtime.createResourceContext(req, resp)));
//    }
//
//    private void respond(HttpServletResponse resp, Supplier<OutboundResponse> supplier) {
//        try {
//            respond(resp, supplier.get());
//        } catch (WebApplicationException exception) {
//            respond(resp, () -> (OutboundResponse) exception.getResponse());
//        } catch (Throwable throwable) {
//            respond(resp, () -> from(throwable));
//        }
//    }
//
//    private void respond(HttpServletResponse resp, OutboundResponse response) throws IOException {
//        resp.setStatus(response.getStatus());
//        MultivaluedMap<String, Object> headers = response.getHeaders();
//        for (String name : headers.keySet()) {
//            for (Object value : headers.get(name)) {
//                RuntimeDelegate.HeaderDelegate headerDelegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
//                resp.addHeader(name, headerDelegate.toString(value));
//            }
//        }
//
//
//        GenericEntity entity = response.getGenericEntity();
//        if (entity != null) {
//            MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
//            writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(), response.getHeaders(), resp.getOutputStream());
//        }
//    }
//
//    private OutboundResponse from(Throwable throwable) {
//        ExceptionMapper mapper = providers.getExceptionMapper(throwable.getClass());
//        return (OutboundResponse) mapper.toResponse(throwable);
//    }
//}
