import { ColumnFilter } from '@openng/optimus-ui/table';
import { FilterMatchMode, FilterMetadata } from '@openng/optimus-ui/api';

export class FilterOverrides {
  static onRowMatchModeChange(this: ColumnFilter, matchMode: string) {
    const fieldFilter = <FilterMetadata>this.dataTable.filters[<string>this.field];
    let filterWasReset = false;
    if ((fieldFilter?.matchMode === FilterMatchMode.BETWEEN) !== (matchMode === FilterMatchMode.BETWEEN)) {
      fieldFilter.value = undefined;
      filterWasReset = true;
    }

    fieldFilter.matchMode = matchMode;

    if (fieldFilter.value != null || filterWasReset) {
      this.dataTable._filter();
    }

    this.hide();
  }
}