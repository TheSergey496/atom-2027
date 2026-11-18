package org.prolegacy.atom2027.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.Subselect;
import org.prolegacy.atom2027.entities.ids.TimeResolveId;

@Subselect("""
select
    id,
    COALESCE(
    NULLIF(
      TRIM(
        COALESCE(NULLIF(EXTRACT(DAY FROM average_resolution_time)::text, '0') || ' д ', '') ||
        COALESCE(NULLIF(EXTRACT(HOUR FROM average_resolution_time)::text, '0') || ' ч ', '') ||
        COALESCE(NULLIF(EXTRACT(MINUTE FROM average_resolution_time)::text, '0') || ' мин', '')
      ),
      '' -- Если после обрезки пробелов строка пустая
    ),
    '0 мин' -- Запасной вариант, если интервал равен 0
  ) AS average_time_str
from time_resolve
""")
@Data
@Setter(AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class TimeResolveStr {
    @Id
    private TimeResolveId id;
    private String averageTimeStr;
}
