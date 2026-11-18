package org.prolegacy.atom2027.second_datasource.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@Table(name = "employee")
public class ForeignEmployee {
    @Id
    private Long tabel;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate startDateExperience;
}
