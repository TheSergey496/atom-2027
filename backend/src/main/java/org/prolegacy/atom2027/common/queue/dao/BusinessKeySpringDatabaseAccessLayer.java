package org.prolegacy.atom2027.common.queue.dao;

import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.support.TransactionOperations;
import ru.yoomoney.tech.dbqueue.config.DatabaseDialect;
import ru.yoomoney.tech.dbqueue.config.QueueTableSchema;
import ru.yoomoney.tech.dbqueue.dao.QueuePickTaskDao;
import ru.yoomoney.tech.dbqueue.settings.FailureSettings;
import ru.yoomoney.tech.dbqueue.settings.QueueLocation;
import ru.yoomoney.tech.dbqueue.spring.dao.SpringDatabaseAccessLayer;

public class BusinessKeySpringDatabaseAccessLayer extends SpringDatabaseAccessLayer {
    @NonNull
    private final String businessKeyColumnName;

    public BusinessKeySpringDatabaseAccessLayer(
            @NonNull DatabaseDialect databaseDialect,
            @NonNull QueueTableSchema queueTableSchema,
            @NonNull JdbcOperations jdbcOperations,
            @NonNull TransactionOperations transactionOperations,
            @NonNull String businessKeyColumnName) {
        super(databaseDialect, queueTableSchema, jdbcOperations, transactionOperations);
        this.businessKeyColumnName = businessKeyColumnName;
    }

    @Override
    public @NonNull QueuePickTaskDao createQueuePickTaskDao(@NonNull QueueLocation queueLocation, @NonNull FailureSettings failureSettings) {
        final QueuePickTaskDao defaultQueuePickTask = super.createQueuePickTaskDao(queueLocation, failureSettings);
        if (getDatabaseDialect() == DatabaseDialect.POSTGRESQL) {
            return new BusinessKeyPostgresQueuePickTask(getJdbcOperations(), getQueueTableSchema(), queueLocation, failureSettings, businessKeyColumnName);
        }
        return defaultQueuePickTask;
    }
}
