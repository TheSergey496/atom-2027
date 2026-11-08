package org.prolegacy.atom2027.second_datasource.repositories;

import org.prolegacy.atom2027.second_datasource.entities.DummyEntityB;
import org.prolegacy.atom2027.second_datasource.entities.ids.DummyEntityBId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DummyEntityBRepository extends JpaRepository<DummyEntityB, DummyEntityBId> {
}
