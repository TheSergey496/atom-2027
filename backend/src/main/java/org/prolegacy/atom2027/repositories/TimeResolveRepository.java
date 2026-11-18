package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.TimeResolve;
import org.prolegacy.atom2027.entities.ids.TimeResolveId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeResolveRepository extends JpaRepository<TimeResolve, TimeResolveId> {
}
