package org.prolegacy.atom2027.common.queue.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.prolegacy.atom2027.common.queue.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.EnqueueParams;
import ru.yoomoney.tech.dbqueue.api.EnqueueResult;
import ru.yoomoney.tech.dbqueue.api.QueueProducer;
import ru.yoomoney.tech.dbqueue.api.TaskPayloadTransformer;
import ru.yoomoney.tech.dbqueue.api.impl.MonitoringQueueProducer;
import ru.yoomoney.tech.dbqueue.api.impl.ShardingQueueProducer;
import ru.yoomoney.tech.dbqueue.api.impl.SingleQueueShardRouter;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;
import ru.yoomoney.tech.dbqueue.settings.*;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.prolegacy.atom2027.common.queue.QueueConfig.BUSINESS_KEY;

public abstract class BasicQueue<Payload> implements Queue<Payload> {
    @Autowired
    protected QueueShard<DatabaseAccessLayer> queueShard;
    @Autowired
    protected ObjectMapper objectMapper;

    protected QueueProducer<Payload> queueProducer;
    protected QueueConfig queueConfig;
    protected TaskPayloadTransformer<Payload> payloadTransformer;

    public abstract Class<? extends Payload> getPayloadClass();

    @Override
    public EnqueueResult enqueue(@NonNull EnqueueParams<Payload> enqueueParams) {
        return this.enqueue(enqueueParams, null);
    }

    public EnqueueResult enqueue(@NonNull EnqueueParams<Payload> enqueueParams, String businessKey) {
        if (this.queueProducer == null) {
            this.queueProducer = new MonitoringQueueProducer<>(
                    new ShardingQueueProducer<>(
                            this.getQueueConfig(),
                            this.getPayloadTransformer(),
                            new SingleQueueShardRouter<>(queueShard)
                    ),
                    this.getQueueConfig().getLocation().getQueueId()
            );
        }
        if (!enqueueParams.getExtData().containsKey(BUSINESS_KEY) && businessKey != null) {
            final Map<String, String> extData = new LinkedHashMap<>(enqueueParams.getExtData());
            extData.put(BUSINESS_KEY, businessKey);
            enqueueParams.withExtData(extData);
        }
        return this.queueProducer.enqueue(enqueueParams);
    }

    protected ProcessingSettings.@NonNull Builder getProcessingSettings(ProcessingSettings.@NonNull Builder builder) {
        return builder;
    }

    protected PollSettings.@NonNull Builder getPollSettings(PollSettings.@NonNull Builder builder) {
        return builder;
    }

    protected FailureSettings.@NonNull Builder getFailureSettings(FailureSettings.@NonNull Builder builder) {
        return builder;
    }

    protected ReenqueueSettings.@NonNull Builder getReenqueueSettings(ReenqueueSettings.@NonNull Builder builder) {
        return builder;
    }

    protected ExtSettings.@NonNull Builder getExtSettings(ExtSettings.@NonNull Builder builder) {
        return builder;
    }

    @Override
    public @NonNull TaskPayloadTransformer<Payload> getPayloadTransformer() {
        if (payloadTransformer == null) {
            payloadTransformer = new TaskPayloadTransformer<>() {
                @Override
                public @Nullable Payload toObject(@Nullable String payload) {
                    return objectMapper.readValue(payload, BasicQueue.this.getPayloadClass());
                }

                @Override
                public @Nullable String fromObject(@Nullable Payload payload) {
                    if (payload == null) return null;
                    return objectMapper.writeValueAsString(payload);
                }
            };
        }
        return payloadTransformer;
    }

    @Override
    public final @NonNull QueueConfig getQueueConfig() {
        if (queueConfig == null) {
            queueConfig = new QueueConfig(
                    QueueLocation.builder()
                            .withTableName("queue_tasks")
                            .withQueueId(new QueueId(this.getClass().getCanonicalName()))
                            .build(),
                    QueueSettings.builder()
                            .withProcessingSettings(getProcessingSettings(
                                    ProcessingSettings.builder()
                                            .withProcessingMode(ProcessingMode.WRAP_IN_TRANSACTION)
                                            .withThreadCount(1)
                            ).build())
                            .withPollSettings(getPollSettings(PollSettings.builder()
                                    .withBetweenTaskTimeout(Duration.ofMillis(100))
                                    .withNoTaskTimeout(Duration.ofMillis(100))
                                    .withFatalCrashTimeout(Duration.ofSeconds(1))
                            ).build())
                            .withFailureSettings(getFailureSettings(FailureSettings.builder()
                                    .withRetryType(FailRetryType.GEOMETRIC_BACKOFF)
                                    .withRetryInterval(Duration.ofMinutes(1))
                            ).build())
                            .withReenqueueSettings(getReenqueueSettings(ReenqueueSettings.builder()
                                    .withRetryType(ReenqueueRetryType.MANUAL)
                            ).build())
                            .withExtSettings(getExtSettings(ExtSettings.builder().withSettings(new LinkedHashMap<>())).build())
                            .build()
            );
        }
        return queueConfig;
    }
}
