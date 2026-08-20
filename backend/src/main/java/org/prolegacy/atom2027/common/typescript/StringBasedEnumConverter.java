package org.prolegacy.atom2027.common.typescript;

import com.blueveery.springrest2ts.converters.JavaEnumToTsEnumConverter;
import com.blueveery.springrest2ts.converters.NullableTypesStrategy;
import com.blueveery.springrest2ts.converters.TypeMapper;
import com.blueveery.springrest2ts.tsmodel.TSEnum;

class StringBasedEnumConverter extends JavaEnumToTsEnumConverter {
    @Override
    public void convert(Class javaClass, NullableTypesStrategy nullableTypesStrategy) {
        TSEnum tsEnum = (TSEnum) TypeMapper.map(javaClass);
        for (Object enumConstant : javaClass.getEnumConstants()) {
            String name = ((Enum<?>) enumConstant).name();
            tsEnum.add("%s = '%s'".formatted(name, name));
        }
        tsEnum.addAllAnnotations(javaClass.getAnnotations());
        conversionListener.tsScopedTypeCreated(javaClass, tsEnum);
    }
}
