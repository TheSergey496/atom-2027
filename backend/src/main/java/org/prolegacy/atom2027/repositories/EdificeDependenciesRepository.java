package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.EdificeDependency;
import org.prolegacy.atom2027.entities.ids.EdificeDependencyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdificeDependenciesRepository extends JpaRepository<EdificeDependency, EdificeDependencyId> {
}
