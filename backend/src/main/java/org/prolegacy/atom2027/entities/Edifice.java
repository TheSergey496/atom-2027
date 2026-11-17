package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.entities.ids.EdificeId;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Edifice {
    @Id
    private EdificeId id;
    private String name;
    private boolean hasWater;
    private boolean hasEnergy;
    private String district;
    private String street;
    private String houseNumber;
    @Setter(AccessLevel.PUBLIC)
    private Long criticalIndex;
}
