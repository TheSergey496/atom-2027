package org.prolegacy.atom2027.common.query.config;

import com.querydsl.jpa.JPQLTemplates;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JpaQueryFactoryConfig {
    @PersistenceContext(unitName = "default")
    private EntityManager entityManager;

    @Bean
    @Primary
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager);
    }
}
