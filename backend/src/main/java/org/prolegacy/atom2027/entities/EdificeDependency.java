package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.entities.ids.EdificeDependencyId;
import org.prolegacy.atom2027.entities.ids.EdificeId;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class EdificeDependency {
    @Id
    private EdificeDependencyId id;
    private boolean hasWater;
    private boolean hasEnergy;
    private EdificeId targetEdificeId;
    private EdificeId sourceEdificeId;
}
