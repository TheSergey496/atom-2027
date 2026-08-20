package org.prolegacy.atom2027.common.typescript;

import com.blueveery.springrest2ts.converters.JavaEnumToTsEnumConverter;
import com.blueveery.springrest2ts.converters.JavaPackageToTsModuleConverter;
import com.blueveery.springrest2ts.converters.NullableTypesStrategy;
import com.blueveery.springrest2ts.converters.TypeMapper;
import com.blueveery.springrest2ts.tsmodel.TSModule;
import com.blueveery.springrest2ts.tsmodel.TSScopedElement;
import lombok.Getter;
import lombok.Setter;
import org.prolegacy.atom2027.common.intefaces.LocaleEnum;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnumConverter extends JavaEnumToTsEnumConverter {
    @Override
    public boolean preConverted(JavaPackageToTsModuleConverter javaPackageToTsModuleConverter, Class javaClass) {
        if (javaClass.isEnum() && TypeMapper.map(javaClass) == TypeMapper.tsAny) {
            TSModule tsModule = javaPackageToTsModuleConverter.getTsModule(javaClass);
            String baseName = classNameMapper.mapJavaClassNameToTs(javaClass.getSimpleName());

            TSRecordType tsRecord = new TSRecordType(baseName + "Record", tsModule);
            tsModule.addScopedElement(tsRecord);
            TypeMapper.registerTsType(javaClass, tsRecord);

            if (LocaleEnum.class.isAssignableFrom(javaClass)) {
                TSRecordType tsLocaleRecord = new TSRecordType(baseName + "LocaleEnum", tsModule);
                tsModule.addScopedElement(tsLocaleRecord);
                tsRecord.setLocaleRecord(tsLocaleRecord);
            }

            return true;
        }
        return false;
    }

    @Override
    public void convert(Class javaClass, NullableTypesStrategy nullableTypesStrategy) {
        TSRecordType tsRecord = (TSRecordType) TypeMapper.map(javaClass);

        for (Object enumConstant : javaClass.getEnumConstants()) {
            Enum<?> e = (Enum<?>) enumConstant;
            String key = e.name();
            String value = e.toString();

            tsRecord.addField(key, value);

            if (tsRecord.getLocaleRecord() != null) {
                String localeValue = ((LocaleEnum) enumConstant).locale();
                tsRecord.getLocaleRecord().addField(key, localeValue);
            }
        }

        tsRecord.addAllAnnotations(javaClass.getAnnotations());
        conversionListener.tsScopedTypeCreated(javaClass, tsRecord);
    }


    private static class TSRecordType extends TSScopedElement {
        @Getter
        @Setter
        private TSRecordType localeRecord;
        private final Map<String, String> fields = new LinkedHashMap<>();

        public TSRecordType(String name, TSModule module) {
            super(name, module);
        }

        public void addField(String key, String value) {
            String escapedValue = value.replace("'", "\\'");
            fields.put(key, escapedValue);
        }

        @Override
        public void write(BufferedWriter writer) throws IOException {
            writer.write(this.toString());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("export const ").append(getName()).append(" = {\n");
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": '").append(entry.getValue()).append("',\n");
            }
            sb.append("} as const;\n");

            sb.append("export type ").append(getName()).append(" = typeof ").append(getName()).append(";\n");

            return sb.toString();
        }
    }
}
