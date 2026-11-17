package org.prolegacy.atom2027.configs;

import org.prolegacy.atom2027.common.hibernate.DynamicDataSourceRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Import(DynamicDataSourceRegistrar.class)
public class DynamicDataSourceConfig {
    @Configuration
    @EnableJpaRepositories(
            basePackages = "org.prolegacy.atom2027.second_datasource.repositories",
            entityManagerFactoryRef = "secondaryEntityManagerFactory",
            transactionManagerRef = "secondaryTransactionManager")
    static class SecondaryRepositories {
    }
}