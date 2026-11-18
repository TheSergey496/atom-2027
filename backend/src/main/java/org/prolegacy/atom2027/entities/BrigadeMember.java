package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.entities.ids.BrigadeId;
import org.prolegacy.atom2027.entities.ids.BrigadeMemberId;
import org.prolegacy.atom2027.entities.ids.EmployeeId;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class BrigadeMember {
    @Id
    @LongIdGenerator
    private BrigadeMemberId id;

    private BrigadeId brigadeId;
    private EmployeeId employeeId;
}
