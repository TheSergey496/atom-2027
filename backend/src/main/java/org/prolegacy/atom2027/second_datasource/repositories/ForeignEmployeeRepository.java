package org.prolegacy.atom2027.second_datasource.repositories;

import org.prolegacy.atom2027.second_datasource.entities.ForeignEmployee;
import org.prolegacy.atom2027.second_datasource.entities.ForeignEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ForeignEmployeeRepository extends JpaRepository<ForeignEmployee, Long> {
}
