package org.prolegacy.atom2027.repositories;

import org.prolegacy.atom2027.entities.Employee;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, EmployeeId> {
}
