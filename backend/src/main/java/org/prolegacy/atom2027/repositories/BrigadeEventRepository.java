package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.BrigadeEvent;
import org.prolegacy.atom2027.entities.ids.BrigadeEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrigadeEventRepository extends JpaRepository<BrigadeEvent, BrigadeEventId> {
}
