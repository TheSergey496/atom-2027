package org.prolegacy.atom2027.common.query.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.prolegacy.atom2027.common.annotation.SimpleQueryClass;
import org.prolegacy.atom2027.common.util.ClassUtils;
import com.querydsl.core.JoinExpression;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.CollectionPath;
import com.querydsl.core.types.dsl.Expressions;
import lombok.Data;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.*;

public class DtoProjections {

    public static <U> Expression<U> constructDto(JPAQuery<?> query, Class<U> clazz, Expression<?>... overrides) {
        return constructDto(query, clazz, null, overrides);
    }

    public static <U> Expression<U> constructDto(JPAQuery<?> query, Class<U> clazz, EntityPath<?> mainPath, Expression<?>... overrides) {
        EntityPath<?> calculatedMainPath = mainPath != null ? mainPath : query.getCalculatedMainPath();
        ExpressionGroup mainGroup = Optional.ofNullable(mainPath)
                .map(ExpressionGroup::new)
                .orElseGet(() -> new ExpressionGroup(calculatedMainPath));
        for (JoinExpression joinExpression : query.getMetadata().getJoins()) {
            if (joinExpression.getTarget() instanceof EntityPath<?> entityPath) {
                mainGroup.addEntityPath(entityPath);
            }
        }
        Arrays.stream(overrides)
                .filter(Objects::nonNull)
                .forEach(override -> {
                    if (override instanceof Operation<?> overrideOperation && overrideOperation.getOperator() == Ops.ALIAS) {
                        mainGroup.addOverride(overrideOperation.getArg(0), ((Path<?>) overrideOperation.getArg(1)).getMetadata().getName());
                    }
                });

        return getDtoExpression(clazz, null, mainGroup);
    }

    private static <U> Expression<U> getDtoExpression(Class<U> clazz, String fieldName, ExpressionGroup currentLevel) {
        List<Expression<?>> expressionsForDto = new ArrayList<>();
        ReflectionUtils.doWithFields(clazz, field -> {
            if (ClassUtils.isSimpleClass(field.getType())
                    || JsonNode.class.isAssignableFrom(field.getType())
                    || field.getType().isAnnotationPresent(SimpleQueryClass.class)) {
                Optional.ofNullable(currentLevel.getSubExpressions().get(field.getName()))
                        .ifPresent(expressionsForDto::add);
            } else {
                Optional.ofNullable(currentLevel.getSubPaths().get(field.getName()))
                        .ifPresent(subPath -> {
                            Expression<?> subPathExpression = getDtoExpression(field.getType(), field.getName(), subPath);
                            if (subPathExpression != null) expressionsForDto.add(subPathExpression);
                        });
            }
        }, field -> !Modifier.isStatic(field.getModifiers()));
        if (expressionsForDto.isEmpty() && fieldName != null) {
            return null;
        }
        FactoryExpression<U> result = Projections.bean(clazz, expressionsForDto.toArray(new Expression<?>[0])).skipNulls();
        return fieldName != null
                ? Expressions.as(result, fieldName)
                : result;
    }


    @Data
    private static final class ExpressionGroup {
        private Expression<?> expression;
        private final Map<String, ExpressionGroup> subPaths = new HashMap<>();
        private final Map<String, Expression<?>> subExpressions = new HashMap<>();


        public ExpressionGroup(Expression<?> expression) {
            this.expression = expression;
        }

        public void addEntityPath(EntityPath<?> entityPath) {
            ExpressionGroup currentLevel = this;
            if (!entityPath.equals(this.expression)) {
                String alias = entityPath.getMetadata().getName();
                String[] aliasSplit = alias.split("[.$]");
                for (int i = 0; i < aliasSplit.length; i++) {
                    currentLevel = currentLevel.getSubPaths().computeIfAbsent(aliasSplit[i], a -> new ExpressionGroup(null));
                    if (i == aliasSplit.length - 1) {
                        currentLevel.setExpression(entityPath);
                    }
                }
            }
            currentLevel.addSubExpressions(entityPath);
        }

        public void addSubExpressions(EntityPath<?> entityPath) {
            ReflectionUtils.doWithFields(entityPath.getClass(), field -> {
                Object fieldValue = field.get(entityPath);
                if (fieldValue instanceof EntityPath<?> subEntityPath) {
                    this.addEntityPath(subEntityPath);
                } else if (fieldValue instanceof CollectionPath<?, ?> collectionPath) {
                } else if (fieldValue instanceof Path<?> path) {
                    this.subExpressions.put(path.getMetadata().getName(), path);
                }
            }, field -> Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()));
        }

        public void addOverride(Expression<?> expression, String alias) {
            ExpressionGroup currentLevel = this;
            String[] aliasSplit = alias.split("[.$]");
            for (int i = 0; i < aliasSplit.length - 1; i++) {
                currentLevel = currentLevel.getSubPaths()
                        .computeIfAbsent(aliasSplit[i], a -> new ExpressionGroup(null));
            }
            currentLevel.subExpressions.put(
                    aliasSplit[aliasSplit.length - 1],
                    Expressions.operation(
                            expression.getType(),
                            Ops.ALIAS,
                            expression,
                            ExpressionUtils.path(expression.getType(), aliasSplit[aliasSplit.length - 1]),
                            ExpressionUtils.path(expression.getType(), alias) // Bless shadowconsp for this hack allowing us, mere mortals, to preserve full path in order to use in QueryParser
                    )
            );
        }

    }
}
