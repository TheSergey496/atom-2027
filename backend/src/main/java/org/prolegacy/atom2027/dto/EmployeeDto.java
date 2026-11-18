package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.entities.ids.EmployeeId;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EmployeeDto {
    private EmployeeId id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private LocalDate startDateExperience;
    private String qualifications;
}
