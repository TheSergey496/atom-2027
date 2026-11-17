package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.entities.Event;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.prolegacy.atom2027.entities.ids.EventId;
import org.prolegacy.atom2027.entities.ids.EventStatusId;
import org.prolegacy.atom2027.repositories.EventRepository;
import org.prolegacy.atom2027.second_datasource.entities.ForeignEvent;
import org.prolegacy.atom2027.second_datasource.repositories.ForeignEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;

@Service
public class EventService {
    @Autowired
    private ForeignEventRepository foreignEventRepository;
    @Autowired
    private EventRepository eventRepository;

    @Transactional
    public Instant loadNewEvents(Instant lastEventTime) {
        final List<ForeignEvent> newEvents;
        if (lastEventTime != null) {
            newEvents = foreignEventRepository.findAllByOccurredAtAfter(lastEventTime);
        } else {
            newEvents = foreignEventRepository.findAll();
        }

        var ref = new Object() {
            Instant maxTime = lastEventTime;
        };
        eventRepository.saveAll(newEvents.stream().map(
                        f -> {
                            if (ref.maxTime == null || (f.getOccurredAt() != null && ref.maxTime.isBefore(f.getOccurredAt()))) {
                                ref.maxTime = f.getOccurredAt();
                            }

                            return new Event(
                                    new EventId(f.getId()),
                                    new EdificeId(f.getIdEdifice()),
                                    f.getResourceType(),
                                    f.getOccurredAt().minus(3, ChronoUnit.HOURS),
                                    new EventStatusId(f.getIdStatus())
                            );
                        }
                )
                .toList());

        return ref.maxTime;
    }
}
