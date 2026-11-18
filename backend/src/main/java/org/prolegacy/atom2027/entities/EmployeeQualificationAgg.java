package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.Subselect;
import org.prolegacy.atom2027.entities.ids.EmployeeId;

@Subselect("""
select employee_id as employee_id,
       string_agg(case resource_type when 'water' then 'Вода ' when 'energy' then 'Электроэнергия ' end || '(' ||
                  proff::text || ')', ', ' ORDER BY resource_type) as qualifications
from employee_qualification
group by employee_id
""")
@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeQualificationAgg {
    @Id
    private EmployeeId employeeId;
    private String qualifications;
}
