package org.prolegacy.atom2027.queue;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.prolegacy.atom2027.common.queue.impl.BasicQueue;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.Task;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import java.time.Duration;
import java.util.HashMap;
import java.util.Random;

@Component
@Log4j2
public class TestQueue extends BasicQueue<TestQueue.TestQueuePayload> {
    private final Random random = new Random(496);

    @Override
    public @NonNull TaskExecutionResult execute(@NonNull Task<TestQueuePayload> task) {
        final TestQueuePayload payload = task.getPayloadOrThrow();
        if (random.nextBoolean()) {
            log.info(payload);
            return TaskExecutionResult.finish();
        } else {
            return TaskExecutionResult.reenqueue(Duration.ofSeconds(5));
        }
    }

    @Override
    public Class<? extends TestQueuePayload> getPayloadClass() {
        return TestQueuePayload.class;
    }

    @Data
    public static class TestQueuePayload {
        private Integer integer;
        private String string;
        private HashMap<String, Long> jopa;
    }
}
