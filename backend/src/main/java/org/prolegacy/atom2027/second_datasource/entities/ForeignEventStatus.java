package org.prolegacy.atom2027.second_datasource.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@Table(name = "event_statuses")
public class ForeignEventStatus {
    @Id
    @Column(name = "id_status")
    private Short id;
    private String name;
}