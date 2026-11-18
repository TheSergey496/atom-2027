package org.prolegacy.atom2027.readers;

import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.EmployeeQualificationDto;
import org.prolegacy.atom2027.entities.QEmployeeQualification;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EmployeeQualificationReader {
    private static final QEmployeeQualification employeeQualification = QEmployeeQualification.employeeQualification;

    @Autowired
    private JPAQueryFactory queryFactory;

    public List<EmployeeQualificationDto> getEmployeeQualifications(EmployeeId employeeId) {
        return baseQuery(employeeId).fetch();
    }

    private JPAQuery<EmployeeQualificationDto> baseQuery(EmployeeId employeeId) {
        return queryFactory.from(employeeQualification)
                .where(employeeQualification.employeeId.eq(employeeId))
                .selectDto(EmployeeQualificationDto.class);
    }
}
