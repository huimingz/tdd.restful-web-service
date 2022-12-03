package restful.tdd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

public class ConverterTest {
    @Test
    public void should_convert_via_converter_constructor() {
        Assertions.assertEquals(Optional.of(new BigDecimal("1234")), ConverterConstructor.convert(BigDecimal.class, "1234"));
    }

    @Test
    public void should_not_convert_if_no_converter_constructor() {
        Assertions.assertEquals(Optional.empty(), ConverterConstructor.convert(NoConverter.class, "1234"));
    }

    @Test
    public void should_convert_via_converter_factory() {
        Assertions.assertEquals(Optional.of(Converter.Factory), ConverterFactory.convert(Converter.class, "Factory"));
    }

    @Test
    public void should_not_convert_if_no_converter_factory() {
        Assertions.assertEquals(Optional.empty(), ConverterFactory.convert(NoConverter.class, "Factory"));
    }
}

class NoConverter {
    NoConverter valueOf(String value) {
        return new NoConverter();
    }
}
