package org.prolegacy.atom2027.common.auth.entities;

import org.prolegacy.atom2027.common.annotation.LongIdGenerator;
import org.prolegacy.atom2027.common.auth.entities.id.RoleId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class Role {
    @Id
    @LongIdGenerator
    private RoleId id;

    @Column(unique = true)
    private String name;

    private String localeName;

    public Role(String name, String localeName) {
        this.name = name;
        this.localeName = localeName;
    }

}
