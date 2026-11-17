package org.prolegacy.atom2027.readers;

import com.querydsl.core.types.dsl.Expressions;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.EdificeDto;
import org.prolegacy.atom2027.entities.QEdifice;
import org.prolegacy.atom2027.entities.QEvent;
import org.prolegacy.atom2027.entities.QEventStatus;
import org.prolegacy.atom2027.entities.ids.EventStatusId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EdificeReader {
    private static final QEdifice edifice = QEdifice.edifice;
    private static final QEvent waterActiveEvent = new QEvent("waterActiveEvent");
    private static final QEvent ev1 = new QEvent("ev1");
    private static final QEvent ev2 = new QEvent("ev2");
    private static final QEvent energyActiveEvent = new QEvent("energyActiveEvent");
    private static final QEventStatus waterEventStatus = new QEventStatus("waterActiveEvent$eventStatus");
    private static final QEventStatus energyEventStatus = new QEventStatus("energyActiveEvent$eventStatus");

    @Autowired
    private JPAQueryFactory queryFactory;


    public List<EdificeDto> getEdifices(Instant instantAt) {

        JPAQuery<?> maxWaterTimeQuery = queryFactory.from(ev1)
                .where(ev1.edificeId.eq(edifice.id))
                .where(ev1.resourceType.eq("water"));
        if (instantAt != null) {
            maxWaterTimeQuery = maxWaterTimeQuery.where(ev1.occurredAt.loe(instantAt));
        }

        var maxWaterTime = maxWaterTimeQuery.select(ev1.occurredAt.max());

        JPAQuery<?> maxEnergyTimeQuery = queryFactory.from(ev2)
                .where(ev2.edificeId.eq(edifice.id))
                .where(ev2.resourceType.eq("energy"));
        if (instantAt != null) {
            maxEnergyTimeQuery = maxEnergyTimeQuery.where(ev2.occurredAt.loe(instantAt));
        }

        var maxEnergyTime = maxEnergyTimeQuery.select(ev2.occurredAt.max());

        return queryFactory.from(edifice)
                .leftJoin(waterActiveEvent).on(
                        Expressions.allOf(
                                waterActiveEvent.edificeId.eq(edifice.id),
                                waterActiveEvent.occurredAt.eq(maxWaterTime)
                        )
                )
                .leftJoin(energyActiveEvent).on(
                        Expressions.allOf(
                                energyActiveEvent.edificeId.eq(edifice.id),
                                energyActiveEvent.occurredAt.eq(maxEnergyTime)
                        )
                )
                .leftJoin(waterEventStatus).on(waterEventStatus.id.eq(waterActiveEvent.eventStatusId))
                .leftJoin(energyEventStatus).on(energyEventStatus.id.eq(energyActiveEvent.eventStatusId))
                .orderBy(edifice.id.asc())
                .selectDto(
                        EdificeDto.class,
                        Expressions.cases()
                                .when(waterActiveEvent.eventStatusId.eq(new EventStatusId(1L)).or(energyActiveEvent.eventStatusId.eq(new EventStatusId(1L))))
                                .then(Expressions.asString("Затронуто объектов: ").concat(edifice.waterDependantCount.add(edifice.energyDependantCount).stringValue()))
                                .otherwise(Expressions.nullExpression()).as("accidentTooltip")
                )
                .fetch();
    }
}
