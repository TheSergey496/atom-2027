package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.EventStatus;
import org.prolegacy.atom2027.entities.ids.EventStatusId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStatusRepository extends JpaRepository<EventStatus, EventStatusId> {
}
