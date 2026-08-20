package org.prolegacy.atom2027.common.auth.dto;

import org.prolegacy.atom2027.common.auth.entities.id.RoleId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDto {
    private RoleId id;
    private String name;
    private String localeName;
}
