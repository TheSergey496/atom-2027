package org.prolegacy.atom2027.common.util;

import com.google.common.collect.Lists;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;

import java.util.List;
import java.util.Optional;

public class QueryUtils {

    public static <T> Predicate generateInExpression(SimpleExpression<T> variable, List<T> inList) {
        return generateInExpression(variable, inList, true, false);
    }

    public static <T> Predicate generateInExpression(SimpleExpression<T> variable, List<T> inList, boolean nullOrEmptyListBehaviour) {
        return generateInExpression(variable, inList, nullOrEmptyListBehaviour, nullOrEmptyListBehaviour);
    }

    public static <T> Predicate generateInExpression(SimpleExpression<T> variable, List<T> inList, boolean nullBehaviour, boolean emptyListBehaviour) {
        if (Optional.ofNullable(inList).isEmpty()) {
            return nullBehaviour ? Expressions.TRUE.isTrue() : Expressions.TRUE.isFalse();
        } else if (inList.isEmpty()) {
            return emptyListBehaviour ? Expressions.TRUE.isTrue() : Expressions.TRUE.isFalse();
        } else if (inList.size() == 1) {
            return variable.eq(inList.get(0));
        }

        return Expressions.anyOf(
                Lists.partition(inList, 1000)
                        .stream()
                        .map(part -> variable.in(inList))
                        .toArray(BooleanExpression[]::new)
        );
    }
}
