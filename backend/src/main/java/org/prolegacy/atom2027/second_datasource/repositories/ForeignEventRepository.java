package org.prolegacy.atom2027.second_datasource.repositories;

import org.prolegacy.atom2027.second_datasource.entities.ForeignEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ForeignEventRepository extends JpaRepository<ForeignEvent, Long> {
    List<ForeignEvent> findAllByOccurredAtAfter(Instant occurredAtAfter);
}
