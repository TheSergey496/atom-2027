package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.entities.ids.BrigadeId;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Brigade {
    @Id
    @LongIdGenerator
    private BrigadeId id;

    private String name;

    public Brigade(String name) {
        this.name = name;
    }
}
