package org.prolegacy.atom2027.second_datasource.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Audited;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.common.audit.Auditable;
import org.prolegacy.atom2027.second_datasource.entities.ids.DummyEntityBId;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Audited
public class DummyEntityB extends Auditable {
    @Id
    @LongIdGenerator
    private DummyEntityBId id;
    private String dummyString;
    private Long dummyLong;
}
