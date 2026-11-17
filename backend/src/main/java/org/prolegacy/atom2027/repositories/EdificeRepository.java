package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.Edifice;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdificeRepository extends JpaRepository<Edifice, EdificeId> {
}
