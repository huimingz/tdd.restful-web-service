package tdd.di;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CyclicDependenciesFound extends RuntimeException {
    private Set<Component> components = new HashSet<>();

    public CyclicDependenciesFound(Class<?> component, Stack<Component> visiting) {
        this.components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.stream().map(c -> c.type()).toArray(Class<?>[]::new);
    }
}
