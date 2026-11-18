package org.prolegacy.atom2027.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Log4j2
public class EventsLoaderScheduler {
    @Autowired
    private EventService eventService;

    private final AtomicBoolean disabled = new AtomicBoolean(false);
    private final AtomicReference<Instant> lastTime = new AtomicReference<>(null);

//    @Scheduled(fixedRate = 2L, timeUnit = TimeUnit.SECONDS)
//    public void loadNewEvents() {
//        if (disabled.get()) {
//            return;
//        }
//        try {
//            final Instant prevLastTime = this.lastTime.get();
//            final Instant instant = eventService.loadNewEvents(lastTime.get());
//            lastTime.compareAndSet(prevLastTime, instant);
//        } catch (Exception e) {
//            log.error("Ошибка загрузки новых сообщений", e);
//        }
//    }

    public void resetLastTime() {
        this.lastTime.set(null);
    }

    public void setDisabled(boolean value) {
        this.disabled.set(value);
    }
}
