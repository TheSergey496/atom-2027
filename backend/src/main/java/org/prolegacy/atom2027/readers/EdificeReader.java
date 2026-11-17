package org.prolegacy.atom2027.readers;

import com.querydsl.core.types.dsl.Expressions;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.EdificeDto;
import org.prolegacy.atom2027.entities.QEdifice;
import org.prolegacy.atom2027.entities.QEvent;
import org.prolegacy.atom2027.entities.QEventStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EdificeReader {
    private static final QEdifice edifice = QEdifice.edifice;
    private static final QEvent event = new QEvent("activeEvent");
    private static final QEventStatus eventStatus = QEventStatus.eventStatus;

    @Autowired
    private JPAQueryFactory queryFactory;


    public List<EdificeDto> getEdifices(Instant instantAt) {

        JPAQuery<?> maxTimeQuery = queryFactory.from(event)
                .where(event.edificeId.eq(edifice.id));
        if (instantAt != null) {
            maxTimeQuery = maxTimeQuery.where(event.occurredAt.loe(instantAt));
        }

        var maxTime = maxTimeQuery.select(event.occurredAt.max());

        return queryFactory.from(edifice)
                .leftJoin(event).on(
                        Expressions.allOf(
                                event.edificeId.eq(edifice.id),
                                event.occurredAt.eq(maxTime)
                        )
                )
                .leftJoin(eventStatus).on(eventStatus.id.eq(event.eventStatusId))
                .orderBy(edifice.id.asc())
                .selectDto(
                        EdificeDto.class,
                        eventStatus.name.coalesce("Норма").as("activeEvent$eventStatusName")
                )
                .fetch();
    }
}
