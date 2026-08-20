package org.prolegacy.atom2027.common.hibernate;

import org.springframework.lang.NonNull;

import java.util.Objects;

public abstract class LongId extends Number implements Comparable<LongId> {

    private final long value;

    public LongId(long value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Override
    public double doubleValue() {
        return (double) value;
    }

    @Override
    public byte byteValue() {
        return (byte) value;
    }

    @Override
    public short shortValue() {
        return (short) value;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }

    @Override
    public int compareTo(@NonNull LongId other) {
        return Long.compare(this.longValue(), other.longValue());
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LongId that = (LongId) obj;
        return Objects.equals(this.value, that.value);
    }
}
