package org.prolegacy.atom2027.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.prolegacy.atom2027.entities.enums.CheckStatus;
import org.prolegacy.atom2027.entities.enums.CheckType;
import org.prolegacy.atom2027.entities.ids.CheckId;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CheckDto {
    private CheckId checkId;
    private CheckType checkType;
    private EdificeDto edifice;
    private Instant planDate;
    private EmployeeDto responsible;
    private CheckStatus checkStatus;
    private String comment;
    private Instant protocolDate;
}
