package org.prolegacy.atom2027.common.query.lazy;

import com.google.common.base.Strings;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.util.ClassUtils;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PageHelper {
    public <T> PageResponse<T> paginate(JPAQuery<T> query, PageQuery pageQuery) {
        Map<String, Expression<?>> pathMap = QueryParser.getPathMap(query);

        Optional.ofNullable(pageQuery.getRows())
                .orElseThrow(() -> new IllegalArgumentException("Missing 'rows' (rows per page) in pageQuery"));
        Optional.ofNullable(pageQuery.getFirst())
                .orElseThrow(() -> new IllegalArgumentException("Missing 'first' (first row number) in pageQuery"));

        if (pageQuery.getFilters() != null && !pageQuery.getFilters().isEmpty()) {
            query = query.where(generateFiltersExpression(pathMap, pageQuery));
        }

        if ((pageQuery.getMultiSortMeta() != null && !pageQuery.getMultiSortMeta().isEmpty()) || pageQuery.getSortField() != null) {
            query.getMetadata().clearOrderBy();
            query = query.orderBy(generateSortsExpression(pathMap, pageQuery));
        }

        if (pageQuery.getRows() <= 0) {
            return new PageResponse<>(new ArrayList<>(), query.fetchCount());
        }

        QueryResults<T> results = query
                .offset(pageQuery.getFirst())
                .limit(pageQuery.getRows())
                .fetchResults();
        return new PageResponse<>(
                results.getResults(),
                results.getTotal()
        );
    }

    private BooleanExpression generateFiltersExpression(Map<String, Expression<?>> pathMap, PageQuery pageQuery) {
        return Expressions.allOf(
                pageQuery.getFilters()
                        .entrySet()
                        .stream()
                        .filter(entry -> Objects.nonNull(entry.getValue())
                                && !Strings.isNullOrEmpty(entry.getValue().getValue())
                        )
                        .map(entry -> {
                                    String field = entry.getKey();
                                    PageQuery.FilterMeta filterMeta = entry.getValue();
                                    if (Objects.equals(field, "@query")) {
                                        return QueryParser.parseCustomFilterQuery(filterMeta.getValue(), pathMap);
                                    }
                                    if (Objects.equals(field, "global")) {
                                        return generateGlobalFilter(pathMap, filterMeta.getValue());
                                    }
                                    Expression<?> path = Optional.ofNullable(pathMap.get(field))
                                            .orElseThrow(() -> new BusinessLogicException("Unknown path " + field));

                                    // Работаем с Instant как с датой без времени
                                    if (Instant.class.isAssignableFrom(path.getType())) {
                                        path = Expressions.template(
                                                LocalDate.class,
                                                "cast({0} as date)",
                                                path
                                        );
                                    }

                                    Expression<?> filterValueExpression = Expressions.constant(
                                            QueryParser.parseValue(filterMeta.getValue(), path.getType())
                                    );

                                    if (Boolean.class.isAssignableFrom(path.getType())) {
                                        return Expressions.booleanOperation(
                                                Ops.EQ,
                                                path,
                                                filterValueExpression
                                        );
                                    }

                                    switch (filterMeta.getMatchMode()) {
                                        case startsWith -> {
                                            return Expressions.booleanOperation(
                                                    Ops.STARTS_WITH,
                                                    path,
                                                    filterValueExpression
                                            );
                                        }
                                        case contains -> {
                                            return Expressions.booleanOperation(
                                                    Ops.STRING_CONTAINS,
                                                    path,
                                                    filterValueExpression
                                            );
                                        }
                                        case notContains -> {
                                            return Expressions.booleanOperation(
                                                    Ops.STRING_CONTAINS,
                                                    path,
                                                    filterValueExpression
                                            ).not();
                                        }
                                        case endsWith -> {
                                            return Expressions.booleanOperation(
                                                    Ops.ENDS_WITH,
                                                    path,
                                                    filterValueExpression
                                            );
                                        }
                                        case equals, dateIs -> {
                                            return Expressions.booleanOperation(
                                                    Ops.EQ,
                                                    path,
                                                    filterValueExpression
                                            );
                                        }
                                        case notEquals, dateIsNot -> {
                                            return Expressions.booleanOperation(
                                                    Ops.EQ,
                                                    path,
                                                    filterValueExpression
                                            ).not();
                                        }
                                        case lt, dateBefore -> {
                                            return Expressions.booleanOperation(
                                                    Ops.LT,
                                                    path,
                                                    filterValueExpression
                                            );
                                        }
                                        case lte -> {
                                            return Expressions.booleanOperation(
                                                    Ops.LOE,
                                                    path,
                                                    filterValueExpression
                                            );
                                        }
                                        case gt, dateAfter -> {
                                            return Expressions.booleanOperation(
                                                    Ops.GT,
                                                    path,
                                                    filterValueExpression
                                            );
                                        }
                                        case gte -> {
                                            return Expressions.booleanOperation(
                                                    Ops.GOE,
                                                    path,
                                                    filterValueExpression
                                            );
                                        }
                                        default ->
                                                throw new IllegalArgumentException("Unknown matchMode " + filterMeta.getMatchMode());
                                    }
                                }
                        ).toArray(BooleanExpression[]::new)
        );
    }

    private BooleanExpression generateGlobalFilter(Map<String, Expression<?>> pathMap, String value) {
        return Expressions.anyOf(
                pathMap.values()
                        .stream()
                        .filter(path -> ClassUtils.isSimpleClass(path.getType()))
                        .filter(path -> !NullExpression.class.isAssignableFrom(path.getClass()))
                        .map(
                                path -> Expressions.booleanOperation(
                                        Ops.STRING_CONTAINS,
                                        String.class.equals(path.getType())
                                                ? path
                                                : Expressions.stringTemplate("cast({0} as text)", path),
                                        Expressions.constant(value)
                                )
                        ).toArray(BooleanExpression[]::new)
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private OrderSpecifier<?>[] generateSortsExpression(Map<String, Expression<?>> pathMap, PageQuery pageQuery) {
        List<OrderSpecifier> orderSpecifiers = Optional.ofNullable(pageQuery.getMultiSortMeta())
                .orElseGet(ArrayList::new)
                .stream()
                .map(sortMeta -> {
                    Expression<?> path = Optional.ofNullable(pathMap.get(sortMeta.getField()))
                            .orElseThrow(() -> new BusinessLogicException("Unknown path " + sortMeta.getField()));
                    return new OrderSpecifier(
                            Optional.ofNullable(sortMeta.getOrder()).orElse(1) == 1 ? Order.ASC : Order.DESC,
                            path
                    );
                })
                .collect(Collectors.toList());
        if (pageQuery.getSortField() != null) {
            Expression<?> path = Optional.ofNullable(pathMap.get(pageQuery.getSortField()))
                    .orElseThrow(() -> new BusinessLogicException("Unknown path " + pageQuery.getSortField()));
            OrderSpecifier<?> defaultOrderSpecifier = new OrderSpecifier(
                    Optional.ofNullable(pageQuery.getSortOrder()).orElse(1) == 1 ? Order.ASC : Order.DESC,
                    path
            );
            orderSpecifiers.addFirst(defaultOrderSpecifier);
        }

        return orderSpecifiers.toArray(OrderSpecifier[]::new);
    }
}
