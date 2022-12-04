package restful.tdd;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class MethodInvoker {

    private static ValueProvider pathParam = (parameter, uriInfo) -> Optional.ofNullable(parameter.getAnnotation(PathParam.class)).map(a -> uriInfo.getPathParameters().get(a.value()));
    private static ValueProvider queryParam = (parameter, uriInfo) -> Optional.ofNullable(parameter.getAnnotation(QueryParam.class)).map(a -> uriInfo.getQueryParameters().get(a.value()));
    private static List<ValueProvider> providers = List.of(pathParam, queryParam);

    static Object invoke(Method method, ResourceContext context, UriInfoBuilder builder) {
        try {
            UriInfo uriInfo = builder.createUriInfo();

            return method.invoke(builder.getLastMatchedResource(), Arrays.stream(method.getParameters())
                    .map(parameter -> injectParameters(parameter, uriInfo)
                            .or(() -> injectContext(parameter, context, uriInfo))
                            .orElse(null)).toArray(Object[]::new));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof WebApplicationException) throw (WebApplicationException) e.getCause();
            throw new RuntimeException(e);
        }catch (IllegalAccessException  e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<Object> injectParameters(Parameter parameter, UriInfo uriInfo) {
        return providers.stream()
                .map(provider -> provider.provider(parameter, uriInfo))
                .filter(Optional::isPresent)
                .findFirst()
                .flatMap(values -> values.flatMap(v -> convert(parameter, v)));
    }

    private static Optional<Object> injectContext(Parameter parameter, ResourceContext resourceContext, UriInfo uriInfo) {
        if (parameter.getType().equals(ResourceContext.class)) return Optional.of(resourceContext);
        if (parameter.getType().equals(UriInfo.class)) return Optional.of(uriInfo);
        return Optional.of(resourceContext.getResource(parameter.getType()));
    }

    private static Optional<Object> convert(Parameter parameter, List<String> values) {
        return PrimitiveConverter.converter(parameter, values)
                .or(() -> ConverterConstructor.convert(parameter.getType(), values.get(0)))
                .or(() -> ConverterFactory.convert(parameter.getType(), values.get(0)));
    }

    interface ValueProvider {
        Optional<List<String>> provider(Parameter parameter, UriInfo uriInfo);
    }
}

class PrimitiveConverter {

    private static Map<Type, DefaultResourceMethod.ValueConverter> primitives = Map.of(
            int.class, DefaultResourceMethod.ValueConverter.singleValue(Integer::parseInt),
            Double.class, DefaultResourceMethod.ValueConverter.singleValue(Double::parseDouble),
            short.class, DefaultResourceMethod.ValueConverter.singleValue(Short::parseShort),
            float.class, DefaultResourceMethod.ValueConverter.singleValue(Float::parseFloat),
            byte.class, DefaultResourceMethod.ValueConverter.singleValue(Byte::parseByte),
            boolean.class, DefaultResourceMethod.ValueConverter.singleValue(Boolean::parseBoolean),
            String.class, DefaultResourceMethod.ValueConverter.singleValue(s -> s));

    public static Optional<Object> converter(Parameter parameter, List<String> values) {
        return Optional.ofNullable(primitives.get(parameter.getType()))
                .map(c -> c.fromString(values));
    }
}


class ConverterConstructor {

    public static Optional<Object> convert(Class<?> converter, String value) {
        try {
            return Optional.of(converter.getConstructor(String.class).newInstance(value));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}

class ConverterFactory {

    public static Optional<Object> convert(Class<?> converter, String value) {
        try {
            return Optional.of(converter.getMethod("valueOf", String.class).invoke(null, value));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}
