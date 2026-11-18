package org.prolegacy.atom2027.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.entities.ids.EmployeeId;

import java.time.LocalDate;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    @Id
    private EmployeeId id;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate startDateExperience;
}
