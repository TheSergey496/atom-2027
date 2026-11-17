package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.common.annotation.TypescriptModel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypescriptModel
public class EdificyDto {
    private Long id;
    private String name;
    private String resourceTypes;
    private String district;
    private String street;
    private String houseNumber;
}
