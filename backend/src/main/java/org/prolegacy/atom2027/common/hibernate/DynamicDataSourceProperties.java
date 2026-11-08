package org.prolegacy.atom2027.common.hibernate;

import lombok.Data;

import java.util.*;

@Data
public class DynamicDataSourceProperties {
    private Map<String, DataSourceConfig> dynamicDatasources = new LinkedHashMap<>();

    @Data
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private String persistenceUnitName;
        private String[] entityPackages;
        private Map<String, Object> jpaProperties = new HashMap<>();

        private record FlatNode(String prefix, Map<?, ?> map, FlatNode parent) {
        }

        public Map<String, Object> getFlattenedJpaProperties() {
            final Map<String, Object> result = new LinkedHashMap<>();
            final Queue<FlatNode> queue = new ArrayDeque<>();
            queue.add(new FlatNode("", this.jpaProperties, null));

            while (!queue.isEmpty()) {
                FlatNode node = queue.poll();

                for (Map.Entry<?, ?> entry : node.map().entrySet()) {
                    String key = requireValidKey(entry.getKey());
                    String path = node.prefix().isEmpty()
                            ? key
                            : node.prefix() + "." + key;

                    Object value = entry.getValue();

                    if (value instanceof Map<?, ?> nested) {
                        if (nested.isEmpty()) {
                            continue;
                        }

                        if (isAncestor(node, nested)) {
                            throw new IllegalStateException("Circular map reference detected at: " + path);
                        }

                        queue.add(new FlatNode(path, nested, node));
                    } else {
                        if (result.containsKey(path)) {
                            throw new IllegalStateException("Duplicate flattened key: " + path);
                        }

                        result.put(path, value);
                    }
                }
            }

            return Collections.unmodifiableMap(result);
        }

        private static boolean isAncestor(FlatNode node, Map<?, ?> map) {
            for (FlatNode current = node; current != null; current = current.parent()) {
                if (current.map() == map) {
                    return true;
                }
            }
            return false;
        }

        private static String requireValidKey(Object key) {
            if (!(key instanceof String s)) {
                throw new IllegalArgumentException("Map keys must be strings: " + key);
            }

            if (s.isEmpty()) {
                throw new IllegalArgumentException("Map keys must not be empty");
            }

            return s;
        }
    }
}
