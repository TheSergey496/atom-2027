package org.prolegacy.atom2027.common.hibernate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonRowMapper<T> implements RowMapper<T> {
    private final Class<T> targetType;
    private final ObjectMapper objectMapper;

    public JsonRowMapper(Class<T> targetType, ObjectMapper objectMapper) {
        this.targetType = targetType;
        this.objectMapper = objectMapper;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> rowMap = new LinkedHashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = JdbcUtils.convertUnderscoreNameToPropertyName(JdbcUtils.lookupColumnName(metaData, i));
            Object columnValue = JdbcUtils.getResultSetValue(rs, i);
            rowMap.put(columnName, columnValue);
        }

        return objectMapper.convertValue(rowMap, targetType);
    }
}
