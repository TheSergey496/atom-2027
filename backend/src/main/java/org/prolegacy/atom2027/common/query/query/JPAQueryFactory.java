package org.prolegacy.atom2027.common.query.query;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAInsertClause;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityManager;

import java.util.function.Supplier;

public class JPAQueryFactory implements JPQLQueryFactory {
    private final JPQLTemplates templates;
    private final Supplier<EntityManager> entityManager;

    public JPAQueryFactory(EntityManager entityManager) {
        this.entityManager = () -> entityManager;
        this.templates = null;
    }

    public JPAQueryFactory(JPQLTemplates templates, EntityManager entityManager) {
        this.entityManager = () -> entityManager;
        this.templates = templates;
    }

    public JPAQueryFactory(Supplier<EntityManager> entityManager) {
        this.entityManager = entityManager;
        this.templates = null;
    }

    public JPAQueryFactory(JPQLTemplates templates, Supplier<EntityManager> entityManager) {
        this.entityManager = entityManager;
        this.templates = templates;
    }

    @Override
    public JPADeleteClause delete(EntityPath<?> path) {
        return this.templates != null ? new JPADeleteClause(this.entityManager.get(), path, this.templates) : new JPADeleteClause(this.entityManager.get(), path);
    }

    @Override
    public <T> JPAQuery<T> select(Expression<T> expr) {
        return this.query().select(expr);
    }

    @Override
    public JPAQuery<Tuple> select(Expression<?>... exprs) {
        return this.query().select(exprs);
    }

    @Override
    public <T> JPAQuery<T> selectDistinct(Expression<T> expr) {
        return this.select(expr).distinct();
    }

    @Override
    public JPAQuery<Tuple> selectDistinct(Expression<?>... exprs) {
        return this.select(exprs).distinct();
    }

    @Override
    public JPAQuery<Integer> selectOne() {
        return this.select(Expressions.ONE);
    }

    @Override
    public JPAQuery<Integer> selectZero() {
        return this.select(Expressions.ZERO);
    }

    @Override
    public <T> JPAQuery<T> selectFrom(EntityPath<T> from) {
        return this.select(from).from(from);
    }

    @Override
    public JPAQuery<?> from(EntityPath<?> from) {
        return this.query().from(from);
    }

    @Override
    public JPAQuery<?> from(EntityPath<?>... from) {
        return this.query().from(from);
    }

    @Override
    public JPAUpdateClause update(EntityPath<?> path) {
        return this.templates != null ? new JPAUpdateClause(this.entityManager.get(), path, this.templates) : new JPAUpdateClause(this.entityManager.get(), path);
    }

    @Override
    public JPAInsertClause insert(EntityPath<?> path) {
        return this.templates != null ? new JPAInsertClause(this.entityManager.get(), path, this.templates) : new JPAInsertClause(this.entityManager.get(), path);
    }

    @Override
    public JPAQuery<?> query() {
        return this.templates != null ? new JPAQuery<>(this.entityManager.get(), this.templates) : new JPAQuery<>(this.entityManager.get());
    }
}
