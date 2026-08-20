package org.prolegacy.atom2027.common.hibernate;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.internal.NamedBasicTypeImpl;

import java.lang.reflect.Proxy;
import java.util.Properties;

public class TypedGenerator extends SequenceStyleGenerator {
    private JavaType<?> javaType = null;

    @Override
    public void configure(GeneratorCreationContext creationContext, Properties parameters) throws MappingException {
        final Type type = creationContext.getType();
        if (type instanceof BasicType<?> namedBasicType) {
            this.javaType = namedBasicType.getJdbcJavaType();
        } else {
            throw new IllegalStateException("LongIdGenerator is used for unknown BasicType");
        }

        parameters.put(
                "sequence_name",
                ConfigurationHelper.getString("target_table", parameters).replace("\"", "")
                        + "_"
                        + ConfigurationHelper.getString("target_column", parameters)
                        + "_seq"
        );

        Type typeToUse = type;
        if (LongId.class.isAssignableFrom(javaType.getJavaTypeClass())) {
            typeToUse = new NamedBasicTypeImpl<>(LongJavaType.INSTANCE, namedBasicType.getJdbcType(), "long");
        }

        Type finalTypeToUse = typeToUse;

        GeneratorCreationContext wrappedContext = (GeneratorCreationContext) Proxy.newProxyInstance(
                creationContext.getClass().getClassLoader(),
                new Class<?>[]{GeneratorCreationContext.class},
                (_, method, args) -> {
                    if ("getType".equals(method.getName())) {
                        return finalTypeToUse;
                    }
                    return method.invoke(creationContext, args);
                }
        );

        super.configure(wrappedContext, parameters);
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        var value = super.generate(session, object);
        return javaType.wrap(value, null);
    }

    @Override
    public Class<?> getGeneratedType() {
        return javaType != null ? javaType.getJavaTypeClass() : super.getGeneratedType();
    }
}