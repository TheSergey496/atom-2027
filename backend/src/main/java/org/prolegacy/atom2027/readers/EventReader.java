package org.prolegacy.atom2027.readers;

import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.EventDto;
import org.prolegacy.atom2027.entities.QEdifice;
import org.prolegacy.atom2027.entities.QEvent;
import org.prolegacy.atom2027.entities.QEventStatus;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventReader {
    private static final QEdifice edifice = QEdifice.edifice;
    private static final QEvent event = new QEvent("activeEvent");
    private static final QEventStatus eventStatus = QEventStatus.eventStatus;

    @Autowired
    private JPAQueryFactory queryFactory;

    public List<EventDto> getEvents(EdificeId edificeId) {
        var query = queryFactory.from(event)
                .leftJoin(edifice).on(edifice.id.eq(event.edificeId))
                .leftJoin(eventStatus).on(eventStatus.id.eq(event.eventStatusId))
                .selectDto(
                        EventDto.class,
                        eventStatus.name.as("eventStatusName")
                );

        if (edificeId != null) {
            query = query.where(event.edificeId.eq(edificeId));
        }

        return query.fetch();
    }
}
