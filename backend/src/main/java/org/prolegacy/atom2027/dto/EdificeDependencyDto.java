package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.common.annotation.TypescriptModel;
import org.prolegacy.atom2027.entities.ids.EdificeId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypescriptModel
public class EdificeDependencyDto {
    private Long id;
    private Boolean hasWater;
    private Boolean hasEnergy;
    private EdificeId targetEdificeId;
    private EdificeId sourceEdificeId;
}
