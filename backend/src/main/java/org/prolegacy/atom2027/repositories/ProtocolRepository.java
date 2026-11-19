package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.Protocol;
import org.prolegacy.atom2027.entities.ids.ProtocolId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProtocolRepository extends JpaRepository<Protocol, ProtocolId> {
}
