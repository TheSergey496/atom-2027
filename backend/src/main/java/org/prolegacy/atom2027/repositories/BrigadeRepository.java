package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.Brigade;
import org.prolegacy.atom2027.entities.ids.BrigadeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrigadeRepository extends JpaRepository<Brigade, BrigadeId> {
}
