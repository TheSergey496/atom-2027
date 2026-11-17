package org.prolegacy.atom2027.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Log4j2
public class EventsLoaderScheduler {
    @Autowired
    private EventService eventService;

    //TODO: подумать, как обезопасить многопоточку
    private Instant lastTime = null;

    @Scheduled(fixedRate = 1L, timeUnit = TimeUnit.MINUTES)
    public void loadNewEvents() {
        try {
            lastTime = eventService.loadNewEvents(lastTime);
        } catch (Exception e) {
            log.error("Ошибка загрузки новых сообщений", e);
        }
    }
}
