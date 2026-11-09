package org.prolegacy.atom2027.common.hibernate;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class DynamicDataSourceRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry) {
        Map<String, DynamicDataSourceProperties.DataSourceConfig> datasources = Binder.get(environment)
                .bind("app.dynamic-datasources", Bindable.mapOf(String.class, DynamicDataSourceProperties.DataSourceConfig.class))
                .orElseGet(Collections::emptyMap);

        datasources.forEach((name, config) -> {
            String dsName = name + "DataSource";
            String emfName = name + "EntityManagerFactory";
            String tmName = name + "TransactionManager";
            String adapterNm = name + "JpaVendorAdapter";
            String jdbcTemplateName = name + "JdbcTemplate";
            String namedJdbcTemplateName = name + "NamedParameterJdbcTemplate";

            registry.registerBeanDefinition(
                    dsName,
                    BeanDefinitionBuilder.genericBeanDefinition(
                            DataSource.class,
                            () -> DataSourceBuilder.create()
                                    .url(config.getUrl())
                                    .username(config.getUsername())
                                    .password(config.getPassword())
                                    .driverClassName(config.getDriverClassName())
                                    .build()
                    ).getBeanDefinition()
            );

            registry.registerBeanDefinition(
                    adapterNm,
                    BeanDefinitionBuilder
                            .genericBeanDefinition(HibernateJpaVendorAdapter.class)
                            .getBeanDefinition()
            );

            registry.registerBeanDefinition(
                    emfName,
                    BeanDefinitionBuilder
                            .genericBeanDefinition(LocalContainerEntityManagerFactoryBean.class)
                            .addPropertyValue("dataSource", new RuntimeBeanReference(dsName))
                            .addPropertyValue("packagesToScan", config.getEntityPackages())
                            .addPropertyValue("persistenceUnitName", Optional.ofNullable(config.getPersistenceUnitName()).orElse(name))
                            .addPropertyValue("jpaPropertyMap", config.getFlattenedJpaProperties())
                            .addPropertyValue("jpaVendorAdapter", new RuntimeBeanReference(adapterNm))
                            .getBeanDefinition()
            );

            registry.registerBeanDefinition(
                    tmName,
                    BeanDefinitionBuilder
                            .genericBeanDefinition(JpaTransactionManager.class)
                            .addConstructorArgReference(emfName)
                            .getBeanDefinition()
            );

            registry.registerBeanDefinition(
                    jdbcTemplateName,
                    BeanDefinitionBuilder
                            .genericBeanDefinition(JdbcTemplate.class)
                            .addConstructorArgReference(dsName)
                            .getBeanDefinition()
            );

            registry.registerBeanDefinition(
                    namedJdbcTemplateName,
                    BeanDefinitionBuilder
                            .genericBeanDefinition(NamedParameterJdbcTemplate.class)
                            .addConstructorArgReference(dsName)
                            .getBeanDefinition()
            );
        });
    }
}