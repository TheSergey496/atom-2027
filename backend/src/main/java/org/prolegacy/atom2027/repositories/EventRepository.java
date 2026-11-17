package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.Event;
import org.prolegacy.atom2027.entities.ids.EventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, EventId> {
}
