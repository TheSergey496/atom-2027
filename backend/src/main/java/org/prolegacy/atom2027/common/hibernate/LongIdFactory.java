package org.prolegacy.atom2027.common.hibernate;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class LongIdFactory<T extends LongId> {
    private final Class<T> clazz;

    public LongIdFactory(Class<T> clazz) {
        this.clazz = clazz;
    }

    private T newInstance(long value) {
        try {
            return clazz.getDeclaredConstructor(Long.TYPE).newInstance(value);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new IllegalStateException("LongId must have constructor with long parameter");
        }
    }

    public T valueOf(long value) {
        return newInstance(value);
    }

    public T valueOf(int value) {
        return newInstance(value);
    }

    public T valueOf(float value) {
        return newInstance((long) value);
    }

    public T valueOf(double value) {
        return newInstance((long) value);
    }

    public T valueOf(BigInteger value) {
        return newInstance(value.longValue());
    }

    public T valueOf(BigDecimal value) {
        return newInstance(value.longValue());
    }

    public T valueOf(CharSequence value) {
        return newInstance(Long.parseLong(String.valueOf(value)));
    }
}
