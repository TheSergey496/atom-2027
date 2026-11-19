package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.Check;
import org.prolegacy.atom2027.entities.ids.CheckId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckRepository extends JpaRepository<Check, CheckId> {
}
