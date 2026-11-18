package org.prolegacy.atom2027.readers;

import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.BrigadeDto;
import org.prolegacy.atom2027.dto.EventDto;
import org.prolegacy.atom2027.entities.QBrigade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Repository
public class BrigadeReader {

    private static final QBrigade brigade = QBrigade.brigade;

    @Autowired
    private JPAQueryFactory queryFactory;

    @Autowired
    private EventReader eventReader;

    public List<BrigadeDto> getBrigades() {
        List<BrigadeDto> brigades = queryFactory.from(brigade)
                .selectDto(BrigadeDto.class)
                .orderBy(brigade.id.asc())
                .fetch();

        var events = eventReader.getEventsForBrigades();
        long number = 1;
        for (BrigadeDto b : brigades) {
            b.setName("Бригада %s".formatted(number));
            number++;
            List<EventDto> e = new ArrayList<>(events.getOrDefault(b.getId(), Collections.emptyList()));
            e.sort(Comparator.comparing(EventDto::getBrigadeOrderNumber, Comparator.nullsLast(Comparator.naturalOrder())));
            b.setEvents(e);
        }

        return brigades;
    }
}
