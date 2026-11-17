package org.prolegacy.atom2027.second_datasource.repositories;

import org.prolegacy.atom2027.second_datasource.entities.ForeignDependencies;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForeignDependenciesRepository extends JpaRepository<ForeignDependencies, Long> {
}
