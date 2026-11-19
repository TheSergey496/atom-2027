package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.entities.ids.ProtocolId;

import java.time.LocalDateTime;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Protocol {
    @Id
    @LongIdGenerator
    private ProtocolId id;
    private Long checkId;
    private LocalDateTime protocolTime;
    private EmployeeId employeeId;
    private String result;
    private String defects;
    private String conclusion;
    private String recommendations;
}
