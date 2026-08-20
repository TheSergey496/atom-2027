package org.prolegacy.atom2027.common.typescript;

import com.blueveery.springrest2ts.converters.JacksonObjectMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class NullableFieldsMapper extends JacksonObjectMapper {
    public NullableFieldsMapper() {
        setFieldsVisibility(JsonAutoDetect.Visibility.ANY);
    }

    @Override
    public String getPropertyName(Field field) {
        return "%s?".formatted(super.getPropertyName(field));
    }

    @Override
    public String getPropertyName(Method method, boolean isGetter) {
        return "%s?".formatted(super.getPropertyName(method, isGetter));
    }
}
