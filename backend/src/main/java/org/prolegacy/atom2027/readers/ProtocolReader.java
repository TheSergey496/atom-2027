package org.prolegacy.atom2027.readers;

import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.ProtocolDto;
import org.prolegacy.atom2027.entities.QEdifice;
import org.prolegacy.atom2027.entities.QEmployee;
import org.prolegacy.atom2027.entities.QProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProtocolReader {
    private static final QProtocol protocol = QProtocol.protocol;
    private static final QEmployee employee = QEmployee.employee;
    private static final QEdifice edifice = QEdifice.edifice;


    @Autowired
    private JPAQueryFactory queryFactory;

    // TODO: добавить проверки
    public List<ProtocolDto> getProtocols() {
        return queryFactory.from(protocol)
                .leftJoin(employee).on(employee.id.eq(protocol.employeeId))
//                .leftJoin(edifice).on(edifice.id.eq())
                .selectDto(ProtocolDto.class)
                .fetch();
    }
}
