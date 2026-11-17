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
public class EdificeDto {
    private EdificeId id;
    private String name;
    private Boolean hasWater;
    private Boolean hasEnergy;
    private String district;
    private String street;
    private String houseNumber;
    private Long criticalIndex;

    private EventDto waterActiveEvent;
    private EventDto energyActiveEvent;

    private String accidentTooltip;
}
