package org.prolegacy.atom2027.readers;

import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.BrigadeEventDto;
import org.prolegacy.atom2027.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrigadeEventReader {
    private static final QBrigadeEvent brigadeEvent = QBrigadeEvent.brigadeEvent;
    private static final QBrigade brigade = QBrigade.brigade;
    private static final QEvent event = QEvent.event;
    private static final QEdifice edifice = QEdifice.edifice;
    private static final QTimeResolve timeResolve = QTimeResolve.timeResolve;

    @Autowired
    private JPAQueryFactory queryFactory;

    public List<BrigadeEventDto> getBrigadeEvents() {
        return queryFactory.from(brigadeEvent)
                .leftJoin(brigade).on(brigade.id.eq(brigadeEvent.brigadeId))
                .leftJoin(event).on(event.id.eq(brigadeEvent.eventId))
                .leftJoin(edifice).on(edifice.id.eq(event.edificeId))
                .leftJoin(timeResolve).on(timeResolve.edificeId.eq(edifice.id), timeResolve.resourceType.eq(event.resourceType))
                .selectDto(
                        BrigadeEventDto.class,
                        timeResolve.averageResolutionTime.as("averageResolutionTime")
                )
                .orderBy(brigadeEvent.orderNumber.asc(), brigadeEvent.id.asc())
                .fetch();
    }
}
