package org.prolegacy.atom2027.common.hibernate;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.BigIntJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.reflections.Reflections;

import java.util.List;

public class DataTypeRegistrar implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        Reflections reflections = new Reflections("org.prolegacy.atom2027");
        reflections.getSubTypesOf(LongId.class).forEach(clazz -> {
            LongIdJavaType<?> javaType = new LongIdJavaType<>(clazz);
            BasicType<?> basicType = new BasicTypeImpl<>(javaType, BigIntJdbcType.INSTANCE);
            typeContributions.contributeJavaType(javaType);
            typeContributions.getTypeConfiguration().addBasicTypeRegistrationContributions(List.of(new BasicTypeRegistration(basicType, new String[]{clazz.getName()})));
        });

    }
}
