package restful.tdd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

public class ConverterConstructorTest {
    @Test
    public void should_convert_via_converter_constructor() {
        Assertions.assertEquals(Optional.of(new BigDecimal("1234")), ConverterConstructor.convert(BigDecimal.class, "1234"));
    }
}
