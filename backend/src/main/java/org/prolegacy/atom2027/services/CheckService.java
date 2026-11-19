package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.common.exceptions.BusinessLogicException;
import org.prolegacy.atom2027.dto.CheckDto;
import org.prolegacy.atom2027.dto.EdificeDto;
import org.prolegacy.atom2027.dto.EmployeeDto;
import org.prolegacy.atom2027.entities.Check;
import org.prolegacy.atom2027.entities.Edifice;
import org.prolegacy.atom2027.entities.Employee;
import org.prolegacy.atom2027.entities.enums.CheckStatus;
import org.prolegacy.atom2027.entities.ids.CheckId;
import org.prolegacy.atom2027.readers.CheckReader;
import org.prolegacy.atom2027.repositories.CheckRepository;
import org.prolegacy.atom2027.repositories.EdificeRepository;
import org.prolegacy.atom2027.repositories.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CheckService {
    @Autowired
    private EdificeRepository edificeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private CheckRepository checkRepository;
    @Autowired
    private CheckReader checkReader;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CheckDto createCheck(CheckDto dto) {
        var edificeId = Optional.ofNullable(dto.getEdifice())
                .map(EdificeDto::getId)
                .orElseThrow(() -> new BusinessLogicException("Не задан объект для проверки"));
        Edifice edifice = edificeRepository.findById(edificeId)
                .orElseThrow(() -> new BusinessLogicException("Не найден объект с id = %s".formatted(edificeId)));
        var employeeId = Optional.ofNullable(dto.getResponsible())
                .map(EmployeeDto::getId)
                .orElse(null);
        if (employeeId != null) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new BusinessLogicException("Не найден рабочий с id = %s".formatted(edificeId)));
        }

        if (checkRepository.existsByEdificeIdAndCheckStatus(edificeId, CheckStatus.SCHEDULED)) {
                throw new BusinessLogicException(
                        "Для объекта «%s» уже существует назначенная проверка".formatted(edifice.getName())
                );
        }
        Check check = this.checkRepository.save(new Check(dto));
        return checkReader.getCheck(check.getCheckId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CheckDto updateCheck(CheckDto dto) {
        var checkId = Optional.ofNullable(dto.getCheckId())
                .orElseThrow(() -> new BusinessLogicException("Не задан id проверки для обновления"));
        Check check = checkRepository.findById(checkId)
                .orElseThrow(() -> new BusinessLogicException("Не найдена проверка с id = %s".formatted(checkId)));
        var edificeId = Optional.ofNullable(dto.getEdifice())
                .map(EdificeDto::getId)
                .orElseThrow(() -> new BusinessLogicException("Не задан объект для проверки"));
        Edifice edifice = edificeRepository.findById(edificeId)
                .orElseThrow(() -> new BusinessLogicException("Не найден объект с id = %s".formatted(edificeId)));
        var employeeId = Optional.ofNullable(dto.getResponsible())
                .map(EmployeeDto::getId)
                .orElse(null);
        if (employeeId != null) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new BusinessLogicException("Не найден рабочий с id = %s".formatted(edificeId)));
        }

        if (checkRepository.existsByEdificeIdAndCheckStatusAndCheckIdNot(edificeId, CheckStatus.SCHEDULED, check.getCheckId())) {
            throw new BusinessLogicException(
                    "Для объекта «%s» уже существует назначенная проверка".formatted(edifice.getName())
            );
        }
        check.update(dto);
        return checkReader.getCheck(check.getCheckId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CheckDto cancelCheck(CheckId checkId) {
        Check check = checkRepository.findById(checkId)
                .orElseThrow(() -> new BusinessLogicException("Не найдена проверка с id = %s".formatted(checkId)));
        check.cancel();
        return checkReader.getCheck(check.getCheckId());
    }
}
