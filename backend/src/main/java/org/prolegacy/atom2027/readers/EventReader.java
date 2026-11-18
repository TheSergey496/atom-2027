package org.prolegacy.atom2027.readers;

import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.prolegacy.atom2027.common.query.query.DtoProjections;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.EventDto;
import org.prolegacy.atom2027.entities.QBrigadeEvent;
import org.prolegacy.atom2027.entities.QEdifice;
import org.prolegacy.atom2027.entities.QEvent;
import org.prolegacy.atom2027.entities.QEventStatus;
import org.prolegacy.atom2027.entities.ids.BrigadeId;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class EventReader {
    private static final QEdifice edifice = QEdifice.edifice;
    private static final QEvent event = new QEvent("activeEvent");
    private static final QEventStatus eventStatus = QEventStatus.eventStatus;

    private static final QEvent ev1 = new QEvent("ev1");
    private static final QEvent ev2 = new QEvent("ev2");

    private static final QBrigadeEvent brigadeEvent = QBrigadeEvent.brigadeEvent;

    @Autowired
    private JPAQueryFactory queryFactory;

    public List<EventDto> getEvents(EdificeId edificeId, boolean showArchived) {

        JPAQuery<?> maxWaterTimeQuery = queryFactory.from(ev1)
                .where(ev1.edificeId.eq(event.edificeId))
                .where(ev1.resourceType.eq("water"));
        var maxWaterTime = maxWaterTimeQuery.select(ev1.occurredAt.max());

        JPAQuery<?> maxEnergyTimeQuery = queryFactory.from(ev2)
                .where(ev2.edificeId.eq(event.edificeId))
                .where(ev2.resourceType.eq("energy"));
        var maxEnergyTime = maxEnergyTimeQuery.select(ev2.occurredAt.max());

        BooleanExpression isArchived = Expressions.cases()
                .when(event.resourceType.eq("water"))
                .<BooleanExpression>then(ev1.id.isNotNull().and(event.id.ne(ev1.id)))
                .when(event.resourceType.eq("energy"))
                .<BooleanExpression>then(ev2.id.isNotNull().and(event.id.ne(ev2.id)))
                .otherwise(false);

        var query = queryFactory.from(event)
                .leftJoin(edifice).on(edifice.id.eq(event.edificeId))
                .leftJoin(eventStatus).on(eventStatus.id.eq(event.eventStatusId))
                .leftJoin(ev1).on(
                        Expressions.allOf(
                                ev1.edificeId.eq(event.edificeId),
                                ev1.resourceType.eq("water"),
                                ev1.occurredAt.eq(maxWaterTime)
                        ))
                .leftJoin(ev2).on(
                        Expressions.allOf(
                                ev2.edificeId.eq(event.edificeId),
                                ev2.resourceType.eq("energy"),
                                ev2.occurredAt.eq(maxEnergyTime)
                        )
                )
                .selectDto(
                        EventDto.class,
                        isArchived.as("isArchived")
                );

        if (edificeId != null) {
            query = query.where(event.edificeId.eq(edificeId));
        }

        if (showArchived) {
            query = query.where(isArchived.isTrue());
        } else {
            query = query.where(isArchived.isFalse());
        }

        return query.fetch();
    }


    public Map<BrigadeId, List<EventDto>> getEventsForBrigades() {
        return queryFactory.from(event)
                .innerJoin(brigadeEvent).on(brigadeEvent.eventId.eq(event.id))
                .leftJoin(edifice).on(edifice.id.eq(event.edificeId))
                .leftJoin(eventStatus).on(eventStatus.id.eq(event.eventStatusId))
                .selectDto(
                        EventDto.class,
                        brigadeEvent.brigadeId.as("brigadeId"),
                        brigadeEvent.orderNumber.as("brigadeOrderNumber")
                ).stream()
                .collect(Collectors.groupingBy(EventDto::getBrigadeId));
    }
}
