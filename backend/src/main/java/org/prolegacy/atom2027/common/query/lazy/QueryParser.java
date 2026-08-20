package org.prolegacy.atom2027.common.query.lazy;

import lombok.NonNull;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.prolegacy.atom2027.common.hibernate.LongId;
import org.prolegacy.atom2027.common.hibernate.LongIdFactory;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.util.StringToLocalDateConverter;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanOperation;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class QueryParser {
    public static BooleanExpression parseCustomFilterQuery(String filterQuery, JPAQuery<?> query) {
        return parseCustomFilterQuery(filterQuery, getPathMap(query));
    }

    public static BooleanExpression parseCustomFilterQuery(String filterQuery, Map<String, Expression<?>> pathMap) {
        try {
            PlainSelect selectStatement = (PlainSelect) CCJSqlParserUtil.parse("select * from dummy where " + filterQuery);
            net.sf.jsqlparser.expression.Expression whereStatement = selectStatement.getWhere();
            return (BooleanExpression) generateExpression(whereStatement, pathMap, null);
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("Incorrect custom filter query", e);
        }
    }

    public static OrderSpecifier<?>[] parseCustomOrderByQuery(String orderByQuery, JPAQuery<?> query) {
        return parseCustomOrderByQuery(orderByQuery, getPathMap(query));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static OrderSpecifier<?>[] parseCustomOrderByQuery(String orderByQuery, Map<String, Expression<?>> pathMap) {
        try {
            PlainSelect selectStatement = (PlainSelect) CCJSqlParserUtil.parse("select * from dummy order by " + orderByQuery);
            return selectStatement.getOrderByElements().stream()
                    .map(orderByElement -> new OrderSpecifier(
                                    orderByElement.isAsc()
                                            ? Order.ASC
                                            : Order.DESC,
                                    generateExpression(orderByElement.getExpression(), pathMap, null),
                                    Optional.ofNullable(orderByElement.getNullOrdering())
                                            .map(ordering ->
                                                    switch (ordering) {
                                                        case NULLS_FIRST -> OrderSpecifier.NullHandling.NullsFirst;
                                                        case NULLS_LAST -> OrderSpecifier.NullHandling.NullsLast;
                                                    }
                                            ).orElse(OrderSpecifier.NullHandling.Default)
                            )
                    ).toArray(OrderSpecifier[]::new);
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("Incorrect custom filter query", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Expression<?> generateExpression(net.sf.jsqlparser.expression.Expression expression, Map<String, Expression<?>> pathMap, Class<?> currentType) {
        if (expression instanceof AndExpression andExpression) {
            return Expressions.booleanOperation(
                    Ops.AND,
                    generateExpression(andExpression.getLeftExpression(), pathMap, null),
                    generateExpression(andExpression.getRightExpression(), pathMap, null)
            );
        }

        if (expression instanceof OrExpression orExpression) {
            return Expressions.booleanOperation(
                    Ops.OR,
                    generateExpression(orExpression.getLeftExpression(), pathMap, null),
                    generateExpression(orExpression.getRightExpression(), pathMap, null)
            );
        }

        if (expression instanceof ExpressionList<?> expressionList) {
            if (expressionList.isEmpty()) throw new IllegalArgumentException("IN expression is empty");
            Expression<?>[] expressionArray = new Expression<?>[expressionList.size()];
            for (int i = 0; i < expressionList.size(); i++) {
                expressionArray[i] = generateExpression(expressionList.get(i), pathMap, currentType);
            }
            if (currentType == null) {
                return Expressions.list(
                        expressionArray[0].getType(),
                        expressionArray
                );
            } else {
                return Expressions.list(
                        currentType,
                        expressionArray
                );
            }
        }

        if (expression instanceof Column path) {
            if ("true".equalsIgnoreCase(path.getColumnName())) {
                return Expressions.TRUE;
            }
            if ("false".equalsIgnoreCase(path.getColumnName())) {
                return Expressions.FALSE;
            }
            return Optional.ofNullable(pathMap.get(path.getFullyQualifiedName()))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown path " + path.getFullyQualifiedName()));
        }


        if (expression instanceof LongValue longValue) {
            return Expressions.constant(
                    parseValue(
                            longValue.getValue(),
                            currentType == null
                                    ? Long.class
                                    : currentType
                    )
            );
        }

        if (expression instanceof DoubleValue doubleValue) {
            return Expressions.constant(
                    parseValue(
                            doubleValue.getValue(),
                            currentType == null
                                    ? Double.class
                                    : currentType
                    )
            );
        }

        if (expression instanceof StringValue stringValue) {
            return Expressions.asString(stringValue.getValue());
        }

        if (expression instanceof CastExpression castExpression) {
            Expression<?> leftExpression = generateExpression(castExpression.getLeftExpression(), pathMap, null);

            Class<?> type = getCastToClass(currentType, castExpression);

            return Expressions.template(
                    type,
                    "cast({0} as " + castExpression.getColDataType().getDataType() + ")",
                    leftExpression
            );
        }

        if (expression instanceof CaseExpression caseExpression) {
            CaseBuilder caseBuilder = Expressions.cases();

            CaseBuilder.Cases cases = null;
            for (WhenClause whenClause : caseExpression.getWhenClauses()) {
                BooleanExpression whenExpression = (BooleanExpression) generateExpression(whenClause.getWhenExpression(), pathMap, null);
                Expression thenExpression = generateExpression(whenClause.getThenExpression(), pathMap, null);
                cases = caseBuilder.when(whenExpression).then(thenExpression);
            }

            if (cases == null) {
                throw new IllegalArgumentException("Incorrect case when expression");
            }

            if (caseExpression.getElseExpression() != null) {
                Expression<?> elseExpression = generateExpression(caseExpression.getElseExpression(), pathMap, null);
                return cases.otherwise(elseExpression);
            } else {
                return cases.otherwise(Expressions.nullExpression());
            }
        }

        if (expression instanceof EqualsTo equalsTo) {
            Expression<?> leftExpression = generateExpression(equalsTo.getLeftExpression(), pathMap, null);
            return Expressions.booleanOperation(
                    Ops.EQ,
                    leftExpression,
                    generateExpression(equalsTo.getRightExpression(), pathMap, leftExpression.getType())
            );
        }

        if (expression instanceof NotEqualsTo notEqualsTo) {
            Expression<?> leftExpression = generateExpression(notEqualsTo.getLeftExpression(), pathMap, null);
            return Expressions.booleanOperation(
                    Ops.NE,
                    leftExpression,
                    generateExpression(notEqualsTo.getRightExpression(), pathMap, leftExpression.getType())
            );
        }

        if (expression instanceof IsNullExpression isNull) {
            return Expressions.booleanOperation(
                    isNull.isNot() ? Ops.IS_NOT_NULL : Ops.IS_NULL,
                    generateExpression(isNull.getLeftExpression(), pathMap, null)
            );
        }

        if (expression instanceof GreaterThan greaterThan) {
            Expression<?> leftExpression = generateExpression(greaterThan.getLeftExpression(), pathMap, null);
            return Expressions.booleanOperation(
                    Ops.GT,
                    leftExpression,
                    generateExpression(greaterThan.getRightExpression(), pathMap, leftExpression.getType())
            );
        }

        if (expression instanceof GreaterThanEquals greaterThanEquals) {
            Expression<?> leftExpression = generateExpression(greaterThanEquals.getLeftExpression(), pathMap, null);
            return Expressions.booleanOperation(
                    Ops.GOE,
                    leftExpression,
                    generateExpression(greaterThanEquals.getRightExpression(), pathMap, leftExpression.getType())
            );
        }

        if (expression instanceof MinorThan minorThan) {
            Expression<?> leftExpression = generateExpression(minorThan.getLeftExpression(), pathMap, null);
            return Expressions.booleanOperation(
                    Ops.LT,
                    leftExpression,
                    generateExpression(minorThan.getRightExpression(), pathMap, leftExpression.getType())
            );
        }

        if (expression instanceof MinorThanEquals minorThanEquals) {
            Expression<?> leftExpression = generateExpression(minorThanEquals.getLeftExpression(), pathMap, null);
            return Expressions.booleanOperation(
                    Ops.LOE,
                    leftExpression,
                    generateExpression(minorThanEquals.getRightExpression(), pathMap, leftExpression.getType())
            );
        }

        if (expression instanceof InExpression inExpression) {
            Expression<?> leftExpression = generateExpression(inExpression.getLeftExpression(), pathMap, null);
            return Expressions.booleanOperation(
                    inExpression.isNot() ? Ops.NOT_IN : Ops.IN,
                    leftExpression,
                    generateExpression(inExpression.getRightExpression(), pathMap, leftExpression.getType())
            );
        }

        if (expression instanceof Between between) {
            Expression<?> leftExpression = generateExpression(between.getLeftExpression(), pathMap, null);
            BooleanOperation result = Expressions.booleanOperation(
                    Ops.BETWEEN,
                    leftExpression,
                    generateExpression(between.getBetweenExpressionStart(), pathMap, leftExpression.getType()),
                    generateExpression(between.getBetweenExpressionEnd(), pathMap, leftExpression.getType())
            );
            return between.isNot() ? result.not() : result;
        }

        if (expression instanceof LikeExpression likeExpression) {
            Expression<?> leftExpression = generateExpression(likeExpression.getLeftExpression(), pathMap, null);
            BooleanOperation result = Expressions.booleanOperation(
                    Ops.LIKE,
                    leftExpression,
                    generateExpression(likeExpression.getRightExpression(), pathMap, leftExpression.getType())
            );
            return likeExpression.isNot() ? result.not() : result;
        }

        throw new IllegalArgumentException("Unknown token " + expression.toString());
    }

    @NonNull
    private static Class<?> getCastToClass(Class<?> currentType, CastExpression castExpression) {
        Class<?> type = currentType;
        if (type == null) {
            switch (castExpression.getColDataType().getDataType()) {
                case "bigint", "int8" -> type = Long.class;
                case "boolean" -> type = Boolean.class;
                case "date" -> type = LocalDate.class;
                case "varchar", "char", "text" -> type = String.class;
                case "double precision", "float8" -> type = Double.class;
                case "integer", "int", "int4" -> type = Integer.class;
                case "numeric", "decimal" -> type = BigDecimal.class;
                case "real", "float4" -> type = Float.class;
                case "smallint", "int2" -> type = Short.class;
                case "timestamp" -> type = Instant.class;
                default ->
                        throw new IllegalArgumentException("Unknown sql type " + castExpression.getColDataType().getDataType());
            }
        }
        return type;
    }

    public static Map<String, Expression<?>> getPathMap(JPAQuery<?> query) {
        EntityPath<?> mainPath = query.getMainPath();
        Map<String, Expression<?>> pathMap = new HashMap<>();
        Optional.ofNullable((FactoryExpression<?>) query.getMetadata().getProjection())
                .orElseThrow(() -> new IllegalArgumentException("Select was not called"))
                .getArgs().forEach(expr -> {
                    if (expr instanceof Path<?> path) {
                        String alias = path.toString();
                        if (mainPath != null && alias.startsWith(query.getMainPath().toString())) {
                            alias = alias.substring(query.getMainPath().toString().length() + 1);
                        }
                        pathMap.put(alias, path);
                    } else if (expr instanceof Operation<?> aliasOp && aliasOp.getOperator() == Ops.ALIAS) {
                        List<Expression<?>> operationArgs = aliasOp.getArgs();
                        String alias = aliasOp.getArg(1).toString();
                        if (operationArgs.size() == 3) { // shadowconsp help us all
                            alias = aliasOp.getArg(2).toString();
                        }
                        pathMap.put(alias, aliasOp.getArg(0));
                    }
                });
//        Optional.ofNullable(metadata)
//                .ifPresent(meta ->
//                        meta.forEach((fieldName, fieldMeta) -> parseMetadata(fieldName, fieldMeta, pathMap, null))
//                );
        return pathMap;
    }

    // TODO: fix
//    private static void parseMetadata(String rootName, Metadata metadata, Map<String, Expression<?>> pathMap, Expression<?> root) {
//        root = root == null ? pathMap.get(rootName) : root;
//        Optional.ofNullable(root).orElseThrow(IllegalArgumentException::new);
//        switch (metadata.getType()) {
//            case VALUE -> {
//                Expression<?> expr = switch (metadata.getMetadataNodeType()) {
//                    case NULL -> Expressions.nullExpression();
//                    case STRING -> Expressions.stringOperation(Ops.STRING_CAST, root);
//                    case BOOLEAN -> Expressions.booleanTemplate("cast({0} as boolean)", root);
//                    case NUMBER ->
//                            Expressions.numberOperation(BigDecimal.class, Ops.NUMCAST, root, ConstantImpl.create(BigDecimal.class));
//                    case DATE -> Expressions.template(LocalDate.class, "cast({0} as date)", root);
//                    case INSTANT -> Expressions.template(Instant.class, "cast({0} as timestamp)", root);
//                };
//                pathMap.put(rootName, expr);
//            }
//            case OBJECT -> {
//                for (Map.Entry<String, Metadata> entry : metadata.getMetadataObject().entrySet()) {
//                    String fieldName = entry.getKey();
//                    Metadata fieldMetadata = entry.getValue();
//                    String extractFunction = fieldMetadata.getType() == Metadata.Type.VALUE
//                            ? "jsonExtractString"
//                            : "jsonExtract";
//                    parseMetadata(
//                            "%s.%s".formatted(rootName, fieldName),
//                            fieldMetadata,
//                            pathMap,
//                            Expressions.template(
//                                    JsonNode.class,
//                                    "function('%s', {0}, {1})".formatted(extractFunction),
//                                    root,
//                                    Expressions.asString(fieldName)
//                            )
//                    );
//                }
//            }
//            case ARRAY -> {
//            }
//            default -> throw new NotImplementedException();
//        }
//    }

    public static Object parseValue(Object value, Class<?> clazz) {
        ConversionService conversionService = new DefaultConversionService();
        if (LongId.class.isAssignableFrom(clazz)) {
            if (Number.class.isAssignableFrom(value.getClass())) {
                Class<? extends LongId> longIdClazz = clazz.asSubclass(LongId.class);
                Number numberValue = (Number) value;
                LongIdFactory<?> longIdFactory = new LongIdFactory<>(longIdClazz);
                return longIdFactory.valueOf(numberValue.longValue());
            } else if (CharSequence.class.isAssignableFrom(value.getClass())) {
                Class<? extends LongId> longIdClazz = clazz.asSubclass(LongId.class);
                CharSequence charSequenceValue = (CharSequence) value;
                LongIdFactory<?> longIdFactory = new LongIdFactory<>(longIdClazz);
                return longIdFactory.valueOf(charSequenceValue);
            } else {
                throw new BusinessLogicException("Unknown class conversion %s -> %s".formatted(value.getClass(), clazz));
            }
        } else if (LocalDate.class.isAssignableFrom(clazz) && String.class.isAssignableFrom(value.getClass())) {
            return StringToLocalDateConverter.convert((String) value);
        } else if (Instant.class.isAssignableFrom(clazz) && String.class.isAssignableFrom(value.getClass())) {
            return Instant.parse((String) value);
        } else {
            return conversionService.convert(value, clazz);
        }
    }
}
