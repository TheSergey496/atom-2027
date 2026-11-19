package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;
import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.prolegacy.atom2027.dto.CheckDto;
import org.prolegacy.atom2027.dto.EdificeDto;
import org.prolegacy.atom2027.dto.EmployeeDto;
import org.prolegacy.atom2027.entities.enums.CheckStatus;
import org.prolegacy.atom2027.entities.enums.CheckType;
import org.prolegacy.atom2027.entities.ids.CheckId;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.prolegacy.atom2027.entities.ids.EmployeeId;

import java.time.Instant;
import java.util.Optional;

@Entity
@Data
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Check {
    @Id
    private CheckId checkId;

    @Enumerated(EnumType.STRING)
    private CheckType checkType;
    private EdificeId edificeId;
    private Instant planDate;
    private EmployeeId responsibleId;
    @Enumerated(EnumType.STRING)
    private CheckStatus checkStatus;
    private String comment;
    private Instant protocolDate;

    public Check(CheckDto dto) {
        this.checkStatus = CheckStatus.SCHEDULED;
        update(dto);
    }

    private void update(CheckDto dto) {
        this.checkType = Optional.ofNullable(dto.getCheckType())
                .orElseThrow(() -> new BusinessLogicException("Не задан тип проверки"));
        this.edificeId = Optional.ofNullable(dto.getEdifice())
                .map(EdificeDto::getId)
                .orElseThrow(() -> new BusinessLogicException("Не задан объект для проверки"));
        this.planDate = Optional.ofNullable(dto.getPlanDate())
                .orElseThrow(() -> new BusinessLogicException("Не задана плановая дата проверки"));
        if (planDate.isAfter(Instant.now())) {
            throw new BusinessLogicException("Плановая дата обязана быть в будущем");
        }
        this.responsibleId = Optional.ofNullable(dto.getResponsible())
                .map(EmployeeDto::getId)
                .orElse(null);
        this.comment = dto.getComment();
    }
}
