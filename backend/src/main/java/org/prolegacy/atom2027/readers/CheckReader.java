package org.prolegacy.atom2027.readers;

import com.querydsl.core.types.dsl.Expressions;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.dto.CheckDto;
import org.prolegacy.atom2027.entities.QCheck;
import org.prolegacy.atom2027.entities.QEdifice;
import org.prolegacy.atom2027.entities.QEmployee;
import org.prolegacy.atom2027.entities.enums.CheckStatus;
import org.prolegacy.atom2027.entities.ids.CheckId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CheckReader {
    private static final QCheck check = QCheck.check;
    private static final QEmployee employee = new QEmployee("responsible");
    private static final QEdifice edifice = QEdifice.edifice;

    @Autowired
    private JPAQueryFactory queryFactory;

    public CheckDto getCheck(CheckId checkId) {
        return baseQuery().where(check.checkId.eq(checkId)).fetchFirst();
    }

    public List<CheckDto> getChecks(boolean onlyCancelled, boolean onlyUnfinished) {
        JPAQuery<CheckDto> query = baseQuery();
        if (onlyCancelled) {
            query = query.where(check.checkStatus.eq(CheckStatus.CANCELED));
        } else {
            query = query.where(check.checkStatus.ne(CheckStatus.CANCELED));
        }

        if (onlyUnfinished) {
            query = query.where(check.checkStatus.ne(CheckStatus.FINISHED));
        }

        return query.fetch();
    }

    private JPAQuery<CheckDto> baseQuery() {
        return queryFactory.from(check)
                .leftJoin(edifice).on(edifice.id.eq(check.edificeId))
                .leftJoin(employee).on(employee.id.eq(check.responsibleId))
                .selectDto(
                        CheckDto.class,
                        employee.lastName.concat(" ").concat(employee.firstName).concat(" ").concat(employee.middleName).as("responsible$fullName"),
                        Expressions.cases()
                                .when(check.protocolDate.after(check.planDate)).then(true)
                                .otherwise(false).as("finishedAfterExpiry")
                );
    }
}
