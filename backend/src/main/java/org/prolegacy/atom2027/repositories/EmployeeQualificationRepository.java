package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.Employee;
import org.prolegacy.atom2027.entities.EmployeeQualification;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.entities.ids.EmployeeQualificationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeQualificationRepository extends JpaRepository<EmployeeQualification, EmployeeQualificationId> {
}
