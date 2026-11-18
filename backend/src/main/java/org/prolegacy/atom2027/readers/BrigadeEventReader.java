package org.prolegacy.atom2027.readers;

import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.BrigadeEventDto;
import org.prolegacy.atom2027.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

@Service
public class BrigadeEventReader {
    private static final QBrigadeEvent brigadeEvent = QBrigadeEvent.brigadeEvent;
    private static final QBrigade brigade = QBrigade.brigade;
    private static final QEvent event = QEvent.event;
    private static final QEdifice edifice = QEdifice.edifice;
    private static final QTimeResolve timeResolve = QTimeResolve.timeResolve;
    private static final QTimeResolveStr timeResolveStr = QTimeResolveStr.timeResolveStr;

    @Autowired
    private JPAQueryFactory queryFactory;

    public List<BrigadeEventDto> getBrigadeEvents() {
        List<BrigadeEventDto> events = queryFactory.from(brigadeEvent)
                .leftJoin(brigade).on(brigade.id.eq(brigadeEvent.brigadeId))
                .leftJoin(event).on(event.id.eq(brigadeEvent.eventId))
                .leftJoin(edifice).on(edifice.id.eq(event.edificeId))
                .leftJoin(timeResolve).on(timeResolve.edificeId.eq(edifice.id), timeResolve.resourceType.eq(event.resourceType))
                .leftJoin(timeResolveStr).on(timeResolveStr.id.eq(timeResolve.id))
                .selectDto(
                        BrigadeEventDto.class,
                        timeResolveStr.averageTimeStr.as("averageResolutionTimeStr")
                )
                .orderBy(brigadeEvent.orderNumber.asc(), brigadeEvent.id.asc())
                .fetch();
        for (BrigadeEventDto dto : events) {
            dto.setActualResolutionTimeStr(
                  formatDifference(dto.getStartDate(), dto.getExpectedEndTime())
            );
        }

        return events;
    }

    private static String formatDifference(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }

        Duration duration = Duration.between(start, end);

        if (duration.isNegative()) {
            duration = duration.negated();
        }

        long totalMinutes = duration.toMinutes();

        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes / 60) % 24;
        long minutes = totalMinutes % 60;

        StringJoiner result = new StringJoiner(" ");

        if (days > 0) {
            result.add(days + " д");
        }

        if (hours > 0) {
            result.add(hours + " ч");
        }

        result.add(minutes + " мин");

        return result.toString();
    }
}
