package org.prolegacy.atom2027.common.query.query;

import com.querydsl.core.*;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.AbstractJPAQuery;
import com.querydsl.jpa.impl.JPAProvider;
import jakarta.persistence.EntityManager;
import lombok.Getter;

public class JPAQuery<T> extends AbstractJPAQuery<T, JPAQuery<T>> {

    @Getter
    private EntityPath<?> mainPath = null;

    public JPAQuery() {
        super(null, JPQLTemplates.DEFAULT, new DefaultQueryMetadata());
    }

    public JPAQuery(EntityManager em) {
        super(em, JPAProvider.getTemplates(em), new DefaultQueryMetadata());
    }

    public JPAQuery(EntityManager em, QueryMetadata metadata) {
        super(em, JPAProvider.getTemplates(em), metadata);
    }

    public JPAQuery(EntityManager em, JPQLTemplates templates) {
        super(em, templates, new DefaultQueryMetadata());
    }

    public JPAQuery(EntityManager em, JPQLTemplates templates, QueryMetadata metadata) {
        super(em, templates, metadata);
    }

    @Override
    public JPAQuery<T> clone(EntityManager entityManager, JPQLTemplates templates) {
        JPAQuery<T> q = new JPAQuery<>(entityManager, templates, this.getMetadata().clone());
        q.clone(this);
        return q;
    }

    @Override
    public JPAQuery<T> clone(EntityManager entityManager) {
        return this.clone(entityManager, JPAProvider.getTemplates(entityManager));
    }

    @Override
    public <U> JPAQuery<U> select(Expression<U> expression) {
        return this.selectInternal(null, expression);
    }

    private <U> JPAQuery<U> selectInternal(EntityPath<?> mainPath, Expression<U> expression) {
        this.mainPath = mainPath != null ? mainPath : getCalculatedMainPath();
        queryMixin.setProjection(expression);
        @SuppressWarnings("unchecked") // This is the new type
        JPAQuery<U> newType = (JPAQuery<U>) this;
        return newType;
    }

    @Override
    public JPAQuery<Tuple> select(Expression<?>... expressions) {
        return this.selectInternal(null, expressions);
    }

    private JPAQuery<Tuple> selectInternal(EntityPath<?> mainPath, Expression<?>... expressions) {
        this.mainPath = mainPath != null ? mainPath : getCalculatedMainPath();
        queryMixin.setProjection(expressions);
        @SuppressWarnings("unchecked") // This is the new type
        JPAQuery<Tuple> newType = (JPAQuery<Tuple>) this;
        return newType;
    }

    public <U> JPAQuery<U> selectDto(Class<U> clazz, Expression<?>... overrides) {
        return this.selectInternal(null, DtoProjections.constructDto(this, clazz, overrides));
    }

    public <U> JPAQuery<U> selectDto(Class<U> clazz, EntityPath<?> mainPath, Expression<?>... overrides) {
        return this.selectInternal(mainPath, DtoProjections.constructDto(this, clazz, mainPath, overrides));
    }

    public EntityPath<?> getCalculatedMainPath() {
        return (EntityPath<?>) getMetadata().getJoins().stream()
                .filter(j -> j.getType() == JoinType.DEFAULT)
                .findFirst()
                .map(JoinExpression::getTarget)
                .orElse(null);
    }
}
