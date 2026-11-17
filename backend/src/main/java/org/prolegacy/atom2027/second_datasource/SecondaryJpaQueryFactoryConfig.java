package org.prolegacy.atom2027.second_datasource;

import com.querydsl.jpa.JPQLTemplates;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecondaryJpaQueryFactoryConfig {
    @PersistenceContext(unitName = "secondary")
    private EntityManager entityManager;

    @Bean(name = "secondaryJPAQueryFactory")
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager);
    }
}
