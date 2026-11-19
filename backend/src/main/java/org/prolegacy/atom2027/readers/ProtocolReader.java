package org.prolegacy.atom2027.readers;

import com.querydsl.core.types.dsl.Expressions;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.ProtocolDto;
import org.prolegacy.atom2027.entities.*;
import org.prolegacy.atom2027.entities.enums.CheckType;
import org.prolegacy.atom2027.entities.ids.ProtocolId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProtocolReader {
    private static final QProtocol protocol = QProtocol.protocol;
    private static final QEmployee employee = QEmployee.employee;
    private static final QCheck check = QCheck.check;
    private static final QEdifice edifice = QEdifice.edifice;
    private static final QEmployeeQualificationAgg employeeQualificationAgg = QEmployeeQualificationAgg.employeeQualificationAgg;

    @Autowired
    private JPAQueryFactory queryFactory;

    public ProtocolDto getProtocol(ProtocolId protocolId) {
        return baseQuery().where(protocol.id.eq(protocolId)).fetchFirst();
    }

    public List<ProtocolDto> getProtocols() {
        return baseQuery().orderBy(protocol.id.desc()).fetch();
    }

    private JPAQuery<ProtocolDto> baseQuery() {
        return queryFactory.from(protocol)
                .leftJoin(employee).on(employee.id.eq(protocol.employeeId))
                .leftJoin(employeeQualificationAgg).on(employeeQualificationAgg.employeeId.eq(employee.id))
                .leftJoin(check).on(check.checkId.eq(protocol.checkId))
                .leftJoin(edifice).on(edifice.id.eq(check.edificeId))
                .selectDto(
                        ProtocolDto.class,
                        employeeQualificationAgg.qualifications.as("employee$qualifications"),
                        employee.lastName.concat(" ").concat(employee.firstName).concat(" ").concat(employee.middleName).as("employee$fullName"),
                        Expressions.cases().when(check.checkType.eq(CheckType.NON_PLAN)).then(true).otherwise(false).as("isNotPlanned")
                );
    }
}
