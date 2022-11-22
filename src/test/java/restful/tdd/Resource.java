package restful.tdd;

import java.util.Optional;

interface Resource {
    Optional<ResourceMethod> matches(String path, String method, String[] mediaTypes, UriInfoBuilder builder);
}
