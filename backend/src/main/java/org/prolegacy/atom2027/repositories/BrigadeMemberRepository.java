package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.BrigadeMember;
import org.prolegacy.atom2027.entities.ids.BrigadeMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrigadeMemberRepository extends JpaRepository<BrigadeMember, BrigadeMemberId> {
}
