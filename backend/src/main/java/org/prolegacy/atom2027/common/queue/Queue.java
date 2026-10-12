package org.prolegacy.atom2027.common.queue;

import ru.yoomoney.tech.dbqueue.api.QueueConsumer;
import ru.yoomoney.tech.dbqueue.api.QueueProducer;

public interface Queue<Payload> extends QueueConsumer<Payload>, QueueProducer<Payload> {
}
