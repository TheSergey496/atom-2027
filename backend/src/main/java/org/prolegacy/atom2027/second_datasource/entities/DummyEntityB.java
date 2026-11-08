package org.prolegacy.atom2027.second_datasource.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.second_datasource.entities.ids.DummyEntityBId;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DummyEntityB {
    @Id
    @LongIdGenerator
    private DummyEntityBId id;
    private String dummyString;
    private Long dummyLong;
}
