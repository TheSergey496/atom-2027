package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.entities.ids.EmployeeQualificationId;

import java.math.BigDecimal;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeQualification {
    @Id
    private EmployeeQualificationId id;
    private EmployeeId employeeId;
    private String resourceType;
    private BigDecimal proff;
}
