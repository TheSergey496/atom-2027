package org.prolegacy.atom2027.common.auth.repositories;

import org.prolegacy.atom2027.common.auth.entities.User;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UserId> {

    default Optional<User> findActualUserByEmail(String email) {
        return this.findByEmailAndArchivedIsFalse(email);
    }

    default List<User> findActualUsersWithRoleName(String roleName) {
        return this.findByRolesNameAndArchivedIsFalse(roleName);
    }

    Optional<User> findByEmailAndArchivedIsFalse(String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String username);

    List<User> findByRolesNameAndArchivedIsFalse(String roleName);
}
