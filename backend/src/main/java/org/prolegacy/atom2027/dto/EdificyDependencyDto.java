package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.common.annotation.TypescriptModel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypescriptModel
public class EdificyDependencyDto {
    private Long id;
    private String resourceType;
    private Long idEdificySource;
    private Long idEdificyDependent;
}
