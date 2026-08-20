package org.prolegacy.atom2027.common.hibernate;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;

public class HibernateFunctionContributor implements FunctionContributor {
    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry()
                .register("stringAgg", new StandardSQLFunction("string_agg", StandardBasicTypes.STRING));
        functionContributions.getFunctionRegistry()
                .registerPattern("stringAggDistinct", "string_agg(distinct ?1, ?2)");
        functionContributions.getFunctionRegistry().registerPattern(
                "jsonExtract",
                "?1 -> ?2",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(JsonNode.class, SqlTypes.JSON)
        );
        functionContributions.getFunctionRegistry().registerPattern(
                "jsonExtractString",
                "?1 ->> ?2",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(JsonNode.class, SqlTypes.JSON)
        );
        functionContributions.getFunctionRegistry().registerPattern(
                "getDifferenceSeconds",
                "extract(epoch from ?1 - ?2)",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.LONG)
        );
        functionContributions.getFunctionRegistry().registerPattern(
                "maxNumberOfAttempts",
                "(select max(ac.c) from (select count(*) c from attempt group by task_id) as ac)",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.LONG)
        );
    }
}
