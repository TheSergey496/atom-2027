package org.prolegacy.atom2027.common.query.lazy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PageQuery {
    private Long first;
    private Long rows;

    private Map<String, FilterMeta> filters;
    private List<SortMeta> multiSortMeta;

    private String sortField;
    private Integer sortOrder;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class FilterMeta {
        private String value;
        private MatchMode matchMode;

        public enum MatchMode {
            startsWith,
            contains,
            notContains,
            endsWith,
            equals,
            notEquals,
            lt,
            lte,
            gt,
            gte,
            dateIs,
            dateIsNot,
            dateBefore,
            dateAfter
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static final class SortMeta {
        private String field;
        private Integer order;
    }
}
