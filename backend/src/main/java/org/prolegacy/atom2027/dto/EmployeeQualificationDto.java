package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.entities.ids.EmployeeQualificationId;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EmployeeQualificationDto {
    private EmployeeQualificationId id;
    private EmployeeId employeeId;
    private String resourceType;
    private BigDecimal proff;
}
