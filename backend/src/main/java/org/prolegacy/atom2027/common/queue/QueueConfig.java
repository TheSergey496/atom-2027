package org.prolegacy.atom2027.common.queue;

import org.prolegacy.atom2027.common.queue.dao.BusinessKeySpringDatabaseAccessLayer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yoomoney.tech.dbqueue.config.*;

import java.util.List;

@Configuration
public class QueueConfig {
    public static final String BUSINESS_KEY = "business_key";
    protected final QueueShard<DatabaseAccessLayer> queueShard;

    protected QueueConfig(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate
    ) {
        queueShard = new QueueShard<>(
                new QueueShardId("shard1"),
                new BusinessKeySpringDatabaseAccessLayer(
                        DatabaseDialect.POSTGRESQL,
                        QueueTableSchema.builder().withExtFields(List.of(BUSINESS_KEY)).build(),
                        jdbcTemplate,
                        transactionTemplate,
                        BUSINESS_KEY
                )
        );
    }

    @Bean
    protected QueueShard<DatabaseAccessLayer> queueShard() {
        return this.queueShard;
    }
}
