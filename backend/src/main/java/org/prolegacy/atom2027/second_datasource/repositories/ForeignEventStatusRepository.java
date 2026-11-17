package org.prolegacy.atom2027.second_datasource.repositories;

import org.prolegacy.atom2027.second_datasource.entities.ForeignEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForeignEventStatusRepository extends JpaRepository<ForeignEventStatus, Long> {
}
