package org.prolegacy.atom2027.readers;

import com.querydsl.core.types.dsl.StringExpression;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.EmployeeDto;
import org.prolegacy.atom2027.entities.QBrigadeMember;
import org.prolegacy.atom2027.entities.QEmployee;
import org.prolegacy.atom2027.entities.QEmployeeQualificationAgg;
import org.prolegacy.atom2027.entities.ids.BrigadeId;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EmployeeReader {
    private static final QEmployee employee = QEmployee.employee;
    private static final QEmployeeQualificationAgg employeeQualificationAgg = QEmployeeQualificationAgg.employeeQualificationAgg;
    private static final QBrigadeMember brigadeMember = QBrigadeMember.brigadeMember;

    @Autowired
    private JPAQueryFactory queryFactory;

    public EmployeeDto getEmployee(EmployeeId employeeId) {
        return baseQuery().where(employee.id.eq(employeeId)).fetchFirst();
    }

    public List<EmployeeDto> getEmployees() {
        return baseQuery().fetch();
    }

    private JPAQuery<EmployeeDto> baseQuery() {
        StringExpression fullName = employee.lastName.concat(" ").concat(employee.firstName).concat(" ").concat(employee.middleName);
        return queryFactory.from(employee)
                .leftJoin(employeeQualificationAgg).on(employeeQualificationAgg.employeeId.eq(employee.id))
                .orderBy(fullName.asc().nullsLast())
                .selectDto(
                        EmployeeDto.class,
                        employeeQualificationAgg.qualifications.as("qualifications"),
                        fullName.as("fullName")
                );
    }

    public List<EmployeeDto> getEmployeesForBrigade(BrigadeId brigadeId) {
        return baseQuery()
                .innerJoin(brigadeMember).on(brigadeMember.employeeId.eq(employee.id))
                .where(brigadeMember.brigadeId.eq(brigadeId))
                .fetch();
    }
}
