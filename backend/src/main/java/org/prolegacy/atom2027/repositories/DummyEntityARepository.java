package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.DummyEntityA;
import org.prolegacy.atom2027.entities.ids.DummyEntityAId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DummyEntityARepository extends JpaRepository<DummyEntityA, DummyEntityAId> {
}
