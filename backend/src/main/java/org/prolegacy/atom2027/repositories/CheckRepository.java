package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.Check;
import org.prolegacy.atom2027.entities.enums.CheckStatus;
import org.prolegacy.atom2027.entities.ids.CheckId;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckRepository extends JpaRepository<Check, CheckId> {
    boolean existsByEdificeIdAndCheckStatus(EdificeId edificeId, CheckStatus checkStatus);
    boolean existsByEdificeIdAndCheckStatusAndCheckIdNot(EdificeId edificeId, CheckStatus checkStatus, CheckId checkId);
}
