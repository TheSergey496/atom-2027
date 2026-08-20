package org.prolegacy.atom2027.common.util;

import org.prolegacy.atom2027.common.hibernate.LongId;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class ClassUtils {
    public static boolean isSimpleClass(Class<?> clazz) {
        return clazz.isPrimitive() || BeanUtils.isSimpleValueType(clazz) || LongId.class.isAssignableFrom(clazz);
    }

    public static boolean isSimpleParameterizedType(ParameterizedType parameterizedType) {
        return Collection.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())
                || Map.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())
                || Optional.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())
                || ResponseEntity.class.isAssignableFrom((Class<?>) parameterizedType.getRawType());
    }
}
