package org.prolegacy.atom2027.readers;

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
    private static final QEvent event = QEvent.event;
    private static final QEventStatus eventStatus = QEventStatus.eventStatus;

    @Autowired
    private JPAQueryFactory queryFactory;


    public List<EdificeDto> getEdifices(Instant instantAt) {
        return queryFactory.from(edifice)
                .orderBy(edifice.id.asc())
                .selectDto(EdificeDto.class)
                .fetch();
    }
}
