package org.prolegacy.atom2027.common.queue.dao;

import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yoomoney.tech.dbqueue.api.TaskRecord;
import ru.yoomoney.tech.dbqueue.config.QueueTableSchema;
import ru.yoomoney.tech.dbqueue.dao.QueuePickTaskDao;
import ru.yoomoney.tech.dbqueue.settings.FailRetryType;
import ru.yoomoney.tech.dbqueue.settings.FailureSettings;
import ru.yoomoney.tech.dbqueue.settings.QueueLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class BusinessKeyPostgresQueuePickTask implements QueuePickTaskDao {
    private String pickTaskSql;
    private MapSqlParameterSource pickTaskSqlPlaceholders;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueueTableSchema queueTableSchema;
    private final String businessKeyColumnName;

    public BusinessKeyPostgresQueuePickTask(@Nonnull JdbcOperations jdbcTemplate,
                                            @Nonnull QueueTableSchema queueTableSchema,
                                            @Nonnull QueueLocation queueLocation,
                                            @Nonnull FailureSettings failureSettings,
                                            @NonNull String businessKeyColumnName) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(requireNonNull(jdbcTemplate));
        this.queueTableSchema = requireNonNull(queueTableSchema);
        this.businessKeyColumnName = businessKeyColumnName;
        this.pickTaskSql = createPickTaskSql(queueLocation, failureSettings);
        pickTaskSqlPlaceholders = new MapSqlParameterSource()
                .addValue("queueName", queueLocation.getQueueId().asString())
                .addValue("retryInterval", failureSettings.getRetryInterval().getSeconds());
        failureSettings.registerObserver((_, newValue) -> {
            pickTaskSql = createPickTaskSql(queueLocation, newValue);
            pickTaskSqlPlaceholders = new MapSqlParameterSource()
                    .addValue("queueName", queueLocation.getQueueId().asString())
                    .addValue("retryInterval", newValue.getRetryInterval().getSeconds());
        });
    }

    @Override
    @Nullable
    public TaskRecord pickTask() {
        return jdbcTemplate.execute(pickTaskSql, pickTaskSqlPlaceholders,
                (PreparedStatement ps) -> {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            //noinspection ReturnOfNull
                            return null;
                        }

                        Map<String, String> additionalData = new LinkedHashMap<>();
                        queueTableSchema.getExtFields().forEach(key -> {
                            try {
                                additionalData.put(key, rs.getString(key));
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        return TaskRecord.builder()
                                .withId(rs.getLong(queueTableSchema.getIdField()))
                                .withCreatedAt(getZonedDateTime(rs, queueTableSchema.getCreatedAtField()))
                                .withNextProcessAt(getZonedDateTime(rs, queueTableSchema.getNextProcessAtField()))
                                .withPayload(rs.getString(queueTableSchema.getPayloadField()))
                                .withAttemptsCount(rs.getLong(queueTableSchema.getAttemptField()))
                                .withReenqueueAttemptsCount(rs.getLong(queueTableSchema.getReenqueueAttemptField()))
                                .withTotalAttemptsCount(rs.getLong(queueTableSchema.getTotalAttemptField()))
                                .withExtData(additionalData).build();
                    }
                });
    }

    private String createPickTaskSql(@Nonnull QueueLocation location, @Nonnull FailureSettings failureSettings) {
        final String idField = queueTableSchema.getIdField();
        final String tableName = location.getTableName();
        final String queueNameField = queueTableSchema.getQueueNameField();
        final String nextProcessAtField = queueTableSchema.getNextProcessAtField();
        final String createdAtField = queueTableSchema.getCreatedAtField();
        final String payloadField = queueTableSchema.getPayloadField();
        final String attemptField = queueTableSchema.getAttemptField();
        final String reenqueueAttemptField = queueTableSchema.getReenqueueAttemptField();
        final String totalAttemptField = queueTableSchema.getTotalAttemptField();

        return "WITH cte AS (" +
                "SELECT q." + idField + " " +
                "FROM " + tableName + " q " +
                "WHERE q." + queueNameField + " = :queueName " +
                "  AND q." + nextProcessAtField + " <= now() " +
                "  AND (q." + businessKeyColumnName + " IS NULL OR NOT EXISTS (" +
                "      SELECT 1 " +
                "      FROM " + tableName + " older " +
                "      WHERE older." + queueNameField + " = q." + queueNameField + " " +
                "        AND older." + businessKeyColumnName + " = q." + businessKeyColumnName + " " +
                "        AND (older." + createdAtField + " < q." + createdAtField +
                "               OR older." + createdAtField + " = q." + createdAtField + " AND " + "older." + idField + " < q." + idField +
                "             )" +
                "  )) " +
                " ORDER BY q." + nextProcessAtField + " ASC " +
                "LIMIT 1 " +
                "FOR UPDATE OF q SKIP LOCKED) " +
                "UPDATE " + tableName + " q " +
                "SET " +
                "  " + nextProcessAtField + " = " + getNextProcessTimeSql(failureSettings.getRetryType(), queueTableSchema) + ", " +
                "  " + attemptField + " = " + attemptField + " + 1, " +
                "  " + totalAttemptField + " = " + totalAttemptField + " + 1 " +
                "FROM cte " +
                "WHERE q." + idField + " = cte." + idField + " " +
                "RETURNING q." + idField + ", " +
                "q." + payloadField + ", " +
                "q." + attemptField + ", " +
                "q." + reenqueueAttemptField + ", " +
                "q." + totalAttemptField + ", " +
                "q." + createdAtField + ", " +
                "q." + nextProcessAtField +
                (queueTableSchema.getExtFields().isEmpty() ? "" : queueTableSchema.getExtFields().stream()
                        .map(field -> "q." + field).collect(Collectors.joining(", ", ", ", "")));
    }

    private ZonedDateTime getZonedDateTime(ResultSet rs, String time) throws SQLException {
        return ZonedDateTime.ofInstant(rs.getTimestamp(time).toInstant(), ZoneId.systemDefault());
    }

    @Nonnull
    private String getNextProcessTimeSql(@Nonnull FailRetryType failRetryType, QueueTableSchema queueTableSchema) {
        requireNonNull(failRetryType);
        return switch (failRetryType) {
            case GEOMETRIC_BACKOFF ->
                    "now() + power(2, " + queueTableSchema.getAttemptField() + ") * :retryInterval * INTERVAL '1 SECOND'";
            case ARITHMETIC_BACKOFF ->
                    "now() + (1 + (" + queueTableSchema.getAttemptField() + " * 2)) * :retryInterval * INTERVAL '1 SECOND'";
            case LINEAR_BACKOFF -> "now() + :retryInterval * INTERVAL '1 SECOND'";
        };
    }
}
