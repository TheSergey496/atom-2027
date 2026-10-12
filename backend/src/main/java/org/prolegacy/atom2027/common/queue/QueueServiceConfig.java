package org.prolegacy.atom2027.common.queue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueService;
import ru.yoomoney.tech.dbqueue.config.QueueShard;
import ru.yoomoney.tech.dbqueue.config.impl.LoggingTaskLifecycleListener;
import ru.yoomoney.tech.dbqueue.config.impl.LoggingThreadLifecycleListener;

import java.util.Collections;
import java.util.List;

@Configuration
public class QueueServiceConfig {
    protected final QueueService queueService;

    protected QueueServiceConfig(
            List<Queue<?>> queues,
            QueueShard<DatabaseAccessLayer> queueShard
    ) {
        this.queueService = new QueueService(
                Collections.singletonList(queueShard),
                new LoggingThreadLifecycleListener(),
                new LoggingTaskLifecycleListener()
        );
        for (Queue<?> queue : queues) {
            queueService.registerQueue(queue);
        }
        queueService.start();
    }

    @Bean
    protected QueueService queueService() {
        return this.queueService;
    }
}
