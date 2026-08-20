package org.prolegacy.atom2027.common.auth.readers;

import org.prolegacy.atom2027.common.auth.dto.RoleDto;
import org.prolegacy.atom2027.common.auth.entities.QRole;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class RoleReader {
    private static final QRole role = QRole.role;

    @Autowired
    private JPAQueryFactory queryFactory;

    public List<RoleDto> getRoles() {
        return this.baseQuery().fetch();
    }

    private JPAQuery<RoleDto> baseQuery() {
        return queryFactory
                .from(role)
                .selectDto(RoleDto.class);
    }
}
