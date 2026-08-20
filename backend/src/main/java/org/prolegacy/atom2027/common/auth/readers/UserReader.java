package org.prolegacy.atom2027.common.auth.readers;

import org.prolegacy.atom2027.common.auth.dto.RoleDto;
import org.prolegacy.atom2027.common.auth.dto.UserDto;
import org.prolegacy.atom2027.common.auth.entities.QRole;
import org.prolegacy.atom2027.common.auth.entities.QUser;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.prolegacy.atom2027.common.query.lazy.PageHelper;
import org.prolegacy.atom2027.common.query.lazy.PageQuery;
import org.prolegacy.atom2027.common.query.lazy.PageResponse;
import org.prolegacy.atom2027.common.query.query.DtoProjections;
import org.prolegacy.atom2027.common.query.query.JPAQuery;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.common.util.QueryUtils;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Repository
@Transactional(readOnly = true)
public class UserReader {
    private static final QUser user = QUser.user;
    private static final QUser dummyUser = new QUser("dummyUser");
    private static final QRole role = QRole.role;
    private static final QRole userRole = new QRole("userRole");

    @Autowired
    private JPAQueryFactory queryFactory;

    @Autowired
    private PageHelper pageHelper;

    @Nullable
    public UserDto getUserByEmail(String email) {
        return getUserByEmail(email, false);
    }

    public UserDto getUserByEmail(String email, boolean joinRoles) {
        UserDto userDto = baseQuery()
                .where(user.email.eq(email))
                .fetchFirst();
        if (joinRoles) {
            setRoles(userDto);
        }
        return userDto;
    }

    public UserDto getUser(UserId userId) {
        return getUser(userId, false);
    }

    public UserDto getUser(UserId userId, boolean joinRoles) {
        UserDto userDto = baseQuery()
                .where(user.id.eq(userId))
                .fetchFirst();
        if (joinRoles) {
            setRoles(userDto);
        }

        return userDto;
    }

    public List<UserDto> getUsers() {
        return getUsers(false);
    }

    public List<UserDto> getUsers(boolean joinRoles) {
        List<UserDto> userDtoList = baseQuery().orderBy(user.id.desc()).fetch();
        if (joinRoles) {
            setRoles(userDtoList);
        }
        return userDtoList;
    }

    public PageResponse<UserDto> searchUsers(PageQuery pageQuery) {
        return pageHelper.paginate(baseQuery(), pageQuery);
    }

    private void setRoles(UserDto userDto) {
        if (userDto == null) return;
        userDto.setRoles(
                new HashSet<>(
                        queryFactory
                                .from(user)
                                .innerJoin(user.roles, userRole)
                                .innerJoin(role).on(userRole.id.eq(role.id))
                                .where(user.id.eq(userDto.getId()))
                                .selectDto(RoleDto.class, role)
                                .fetch()
                )
        );
    }

    private void setRoles(List<UserDto> userDtoList) {
        JPAQuery<?> rolesQuery = queryFactory
                .from(user)
                .innerJoin(user.roles, userRole)
                .innerJoin(role).on(userRole.id.eq(role.id))
                .where(QueryUtils.generateInExpression(user.id, userDtoList.stream().map(UserDto::getId).toList()));
        Map<UserId, List<RoleDto>> rolesByUser = rolesQuery.transform(
                GroupBy.groupBy(user.id)
                        .as(GroupBy.list(DtoProjections.constructDto(rolesQuery, RoleDto.class, role)))
        );
        userDtoList.forEach(
                user -> user.setRoles(new HashSet<>(rolesByUser.getOrDefault(user.getId(), new ArrayList<>())))
        );
    }

    private JPAQuery<UserDto> baseQuery() {
        JPAQuery<String> rolesAsString = queryFactory
                .from(dummyUser)
                .innerJoin(dummyUser.roles, userRole)
                .innerJoin(role).on(userRole.id.eq(role.id))
                .where(dummyUser.id.eq(user.id))
                .select(Expressions.stringTemplate("function('stringAgg', {0}, ', ')", role.localeName).stringValue());


        return queryFactory
                .from(user)
                .selectDto(
                        UserDto.class,
                        Expressions.as(Expressions.nullExpression(String.class), "password"),
                        getShortName(user).as("shortName"),
                        getFullName(user).as("fullName"),
                        Expressions.as(rolesAsString, "rolesAsString")
                );
    }

    public static StringExpression getFullName(QUser qUser) {
        return qUser.firstname.concat(" ")
                .concat(
                        Expressions.cases()
                                .when(qUser.surname.isNotNull())
                                .then(qUser.surname.concat(" ").concat(qUser.lastname))
                                .otherwise(qUser.lastname)
                );
    }

    public static StringExpression getShortName(QUser qUser) {
        return qUser.firstname.concat(" ")
                .concat(
                        Expressions.cases()
                                .when(qUser.surname.isNotNull())
                                .then(qUser.surname.substring(0, 1).concat(" ").concat(qUser.lastname.substring(0, 1)))
                                .otherwise(qUser.lastname.substring(0, 1))
                );
    }
}
