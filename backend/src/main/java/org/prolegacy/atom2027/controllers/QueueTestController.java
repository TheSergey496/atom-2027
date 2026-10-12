package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.queue.TestQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yoomoney.tech.dbqueue.api.EnqueueParams;

import java.util.Random;

@RestController()
@RequestMapping("queue-test")
@TypescriptEndpoint
public class QueueTestController {
    @Autowired
    private TestQueue testQueue;

    private final Random random = new Random(666);

    @PostMapping
    public void postMessage(@RequestBody TestQueue.TestQueuePayload payload) {
        testQueue.enqueue(EnqueueParams.create(payload), random.nextBoolean() ? null : "jopa");
    }
}
