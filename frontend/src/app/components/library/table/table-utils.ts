import { FilterMatchMode, SortMeta } from '@openng/optimus-ui/api';
import { Column, ColumnUtils, TableColumn } from './column';
import { TableFilterEvent } from '@openng/optimus-ui/table';
import { CustomFilterMatchMode } from './custom-filter-match-mode';

export class TableUtils {
  public static generateAppliedFilters(
    event: TableFilterEvent,
    numberFormatterCache: { [key: string]: Intl.NumberFormat },
    columnsByField: { [key: string]: TableColumn },
  ): {
    hasFilter: boolean;
    appliedFilters: { column: TableColumn; label: string; valueLocale: string }[];
  } {
    const filters = event?.filters ?? {};
    let hasFilter = false;
    let appliedFilters = [];

    for (let filterField of Object.keys(filters)) {
      if (filterField === 'global') {
        continue;
      }

      const filter = filters[filterField];
      if (filter?.value == null || filter?.value === '' || filter?.matchMode == null) {
        continue;
      }

      const column = columnsByField[filterField];
      if (column == null) {
        continue;
      }
      hasFilter = true;

      let matchModeLocale = '';
      switch (filter.matchMode) {
        case FilterMatchMode.STARTS_WITH: {
          matchModeLocale = 'начинается с';
          break;
        }
        case FilterMatchMode.CONTAINS: {
          matchModeLocale = 'содержит';
          break;
        }
        case FilterMatchMode.NOT_CONTAINS: {
          matchModeLocale = 'не содержит';
          break;
        }
        case FilterMatchMode.ENDS_WITH: {
          matchModeLocale = 'заканчивается на';
          break;
        }
        case FilterMatchMode.EQUALS:
        case FilterMatchMode.IS:
        case FilterMatchMode.DATE_IS:
        case CustomFilterMatchMode.NULLABLE_EQUALS: {
          matchModeLocale = 'равно';
          break;
        }
        case FilterMatchMode.NOT_EQUALS:
        case FilterMatchMode.IS_NOT:
        case FilterMatchMode.DATE_IS_NOT:
        case CustomFilterMatchMode.NULLABLE_NOT_EQUALS: {
          matchModeLocale = 'не равно';
          break;
        }
        case FilterMatchMode.IN: {
          matchModeLocale = 'одно из';
          break;
        }
        case FilterMatchMode.LESS_THAN:
        case FilterMatchMode.BEFORE:
        case FilterMatchMode.DATE_BEFORE: {
          matchModeLocale = 'меньше';
          break;
        }
        case FilterMatchMode.LESS_THAN_OR_EQUAL_TO: {
          matchModeLocale = 'меньше или равно';
          break;
        }
        case FilterMatchMode.GREATER_THAN:
        case FilterMatchMode.AFTER:
        case FilterMatchMode.DATE_AFTER: {
          matchModeLocale = 'больше';
          break;
        }
        case FilterMatchMode.GREATER_THAN_OR_EQUAL_TO: {
          matchModeLocale = 'больше или равно';
          break;
        }
        case FilterMatchMode.BETWEEN: {
          matchModeLocale = 'между';
          break;
        }
      }

      const fieldLocale = column?.originalColumn?.header;

      let valueLocale = '';

      switch (column?.originalColumn?.type) {
        case 'int':
        case 'currency':
        case 'number': {
          const formatter = ColumnUtils.getNumberFormatter(
            numberFormatterCache,
            undefined,
            20,
            true,
          );
          valueLocale = formatter.format(filter.value as number);
          break;
        }
        case 'boolean': {
          switch (filter.value) {
            case true: {
              valueLocale = 'Да';
              break;
            }
            case CustomFilterMatchMode.nullReservedFilterWord: {
              valueLocale = 'Не указано';
              break;
            }
            default: {
              valueLocale = 'Нет';
              break;
            }
          }
          break;
        }
        case 'text': {
          valueLocale = `${filter.value}`;
          break;
        }
        case 'date': {
          switch (filter.matchMode) {
            case FilterMatchMode.BETWEEN: {
              const firstDate =
                filter.value[0] != null
                  ? filter.value[0].toLocaleDateString('ru-RU', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                    })
                  : '';
              const secondDate =
                filter.value[1] != null
                  ? filter.value[1].toLocaleDateString('ru-RU', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                    })
                  : '';
              valueLocale = `${firstDate} и ${secondDate}`;
              break;
            }
            default: {
              valueLocale = filter.value.toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
              });
              break;
            }
          }
          break;
        }
        case 'enum': {
          switch (filter.value) {
            case CustomFilterMatchMode.nullReservedFilterWord: {
              valueLocale = 'Не указано';
              break;
            }
            default: {
              valueLocale = `${((column?.originalColumn as Extract<Column, { type: 'enum' }>)?.locale ?? {})[filter.value]}`;
              break;
            }
          }
          break;
        }
      }

      switch (filter.matchMode) {
        case FilterMatchMode.STARTS_WITH:
        case FilterMatchMode.CONTAINS:
        case FilterMatchMode.NOT_CONTAINS:
        case FilterMatchMode.ENDS_WITH: {
          break;
        }
        case FilterMatchMode.EQUALS:
        case FilterMatchMode.IS:
        case FilterMatchMode.DATE_IS:
        case CustomFilterMatchMode.NULLABLE_EQUALS: {
          matchModeLocale = 'равно';
          break;
        }
        case FilterMatchMode.NOT_EQUALS:
        case FilterMatchMode.IS_NOT:
        case FilterMatchMode.DATE_IS_NOT:
        case CustomFilterMatchMode.NULLABLE_NOT_EQUALS: {
          matchModeLocale = 'не равно';
          break;
        }
        case FilterMatchMode.IN: {
          matchModeLocale = 'одно из';
          break;
        }
        case FilterMatchMode.LESS_THAN:
        case FilterMatchMode.BEFORE:
        case FilterMatchMode.DATE_BEFORE: {
          matchModeLocale = 'меньше';
          break;
        }
        case FilterMatchMode.LESS_THAN_OR_EQUAL_TO: {
          matchModeLocale = 'меньше или равно';
          break;
        }
        case FilterMatchMode.GREATER_THAN:
        case FilterMatchMode.AFTER:
        case FilterMatchMode.DATE_AFTER: {
          matchModeLocale = 'больше';
          break;
        }
        case FilterMatchMode.GREATER_THAN_OR_EQUAL_TO: {
          matchModeLocale = 'больше или равно';
          break;
        }
        case FilterMatchMode.BETWEEN: {
          matchModeLocale = 'между';
          break;
        }
      }

      appliedFilters.push({
        column: column,
        label: `${fieldLocale} ${matchModeLocale}`,
        valueLocale: valueLocale,
      });
    }

    return {
      hasFilter: hasFilter,
      appliedFilters: appliedFilters,
    };
  }
}

export interface SortEvent {
  data: any[];
  sortMode: 'single' | 'multiple';
  field: string;
  order: number;
  multiSortMeta: SortMeta[];
}
