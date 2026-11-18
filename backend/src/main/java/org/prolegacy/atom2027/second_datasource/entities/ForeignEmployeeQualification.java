package org.prolegacy.atom2027.second_datasource.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.entities.ids.EmployeeQualificationId;

import java.math.BigDecimal;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@Table(name = "qualification")
public class ForeignEmployeeQualification {
    @Id
    private Long idQualification;
    private Long idEmployee;
    private String resourceType;
    private BigDecimal proff;
}
