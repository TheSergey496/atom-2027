package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.entities.ids.DummyEntityAId;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DummyEntityA {
    @Id
    @LongIdGenerator
    private DummyEntityAId id;
    private String dummyString;
    private Long dummyLong;
}
