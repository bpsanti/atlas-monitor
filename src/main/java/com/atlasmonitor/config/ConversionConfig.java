package com.atlasmonitor.config;

import com.atlasmonitor.converter.BidirectionalConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class ConversionConfig implements WebMvcConfigurer {

    private final Set<Converter<?, ?>> converters;
    private final Set<BidirectionalConverter<?, ?>> bidirectionalConverters;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        converters.forEach(registry::addConverter);
        bidirectionalConverters.forEach(bc -> registry.addConverter(toGenericConverter(bc)));
    }

    @SuppressWarnings("unchecked")
    private <A, B> GenericConverter toGenericConverter(BidirectionalConverter<A, B> bc) {
        var types = resolveTypes(bc);

        return new GenericConverter() {
            @Override
            public Set<ConvertiblePair> getConvertibleTypes() {
                return Set.of(
                    new ConvertiblePair(types[0], types[1]),
                    new ConvertiblePair(types[1], types[0])
                );
            }

            @Override
            public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
                if (types[0].isInstance(source)) {
                    return ((BidirectionalConverter<Object, Object>) bc).convertTo(source);
                }
                return ((BidirectionalConverter<Object, Object>) bc).convertFrom((Object) source);
            }
        };
    }

    private Class<?>[] resolveTypes(BidirectionalConverter<?, ?> bc) {
        for (var iface : bc.getClass().getGenericInterfaces()) {
            if (iface instanceof java.lang.reflect.ParameterizedType pt
                && pt.getRawType() == BidirectionalConverter.class) {
                return new Class<?>[] {
                    (Class<?>) pt.getActualTypeArguments()[0],
                    (Class<?>) pt.getActualTypeArguments()[1]
                };
            }
        }
        throw new IllegalStateException("Cannot resolve type arguments for " + bc.getClass());
    }
}
