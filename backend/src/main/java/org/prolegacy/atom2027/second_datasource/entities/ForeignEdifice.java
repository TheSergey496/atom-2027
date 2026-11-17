package org.prolegacy.atom2027.second_datasource.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@Table(name = "edifices")
public class ForeignEdifice {
    @Id
    @Column(name = "id_edifice")
    private Long id;
    private String name;
    private String resourceType;
    private String district;
    private String street;
    private String houseNumber;
}
