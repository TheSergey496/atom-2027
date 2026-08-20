package org.prolegacy.atom2027.common.hibernate;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Objects;

public class LongIdJavaType<T extends LongId> extends AbstractClassJavaType<T> {
    private final Class<T> clazz;
    private final LongIdFactory<T> factory;

    public LongIdJavaType(Class<T> clazz) {
        super(clazz);
        this.clazz = clazz;
        this.factory = new LongIdFactory<>(clazz);
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
        return indicators.getTypeConfiguration()
                .getJdbcTypeRegistry()
                .getDescriptor(Types.BIGINT);
    }

    @Override
    public String toString(LongId value) {
        return value.toString();
    }

    @Override
    public T fromString(CharSequence string) {
        return factory.valueOf(string);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }

        if (Long.class.isAssignableFrom(type)) {
            return (X) Long.valueOf(value.longValue());
        }
        if (Integer.class.isAssignableFrom(type)) {
            return (X) Integer.valueOf(value.intValue());
        }
        if (Float.class.isAssignableFrom(type)) {
            return (X) Float.valueOf(value.floatValue());
        }
        if (Double.class.isAssignableFrom(type)) {
            return (X) Double.valueOf(value.doubleValue());
        }
        if (BigInteger.class.isAssignableFrom(type)) {
            return (X) BigInteger.valueOf(value.longValue());
        }
        if (BigDecimal.class.isAssignableFrom(type)) {
            return (X) BigDecimal.valueOf(value.longValue());
        }
        if (CharSequence.class.isAssignableFrom(type)) {
            return (X) toString(value);
        }

        if (LongId.class.isAssignableFrom(type)) {
            return (X) value;
        }

        throw unknownUnwrap(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> T wrap(X value, WrapperOptions wrapperOptions) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long longValue) {
            return factory.valueOf(longValue);
        }
        if (value instanceof Integer intValue) {
            return factory.valueOf(intValue);
        }
        if (value instanceof Float floatValue) {
            return factory.valueOf(floatValue);
        }
        if (value instanceof Double doubleValue) {
            return factory.valueOf(doubleValue);
        }
        if (value instanceof BigInteger bigIntegerValue) {
            return factory.valueOf(bigIntegerValue);
        }
        if (value instanceof BigDecimal bigDecimalValue) {
            return factory.valueOf(bigDecimalValue);
        }
        if (value instanceof CharSequence) {
            return fromString((CharSequence) value);
        }

        if (clazz.isInstance(value)) {
            return (T) value;
        }

        throw unknownWrap(value.getClass());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LongIdJavaType<?> that = (LongIdJavaType<?>) o;
        return Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz);
    }
}
