package org.prolegacy.atom2027.second_datasource.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Entity
@Table(name = "edifice_dependencies")
@Data
@Setter(AccessLevel.PRIVATE)
public class ForeignDependencies {
    @Id
    @Column(name = "id_dependence")
    private Integer id;
    @Column(name = "resource_type")
    private String resourceType;
    private Long idEdificeDependent;
    private Long idEdificeSource;
}
