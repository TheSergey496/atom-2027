import { DomHandler } from '@openng/optimus-ui/dom';
import { ObjectUtils } from '@openng/optimus-ui/utils';
import { Table } from '@openng/optimus-ui/table';
import { ElementRef } from '@angular/core';
import { FilterMatchMode, FilterMetadata, FilterOperator } from '@openng/optimus-ui/api';
import { Column, ColumnUtils } from './column';

export class TableOverrides {
  static onColumnDrop(this: Table, event: Event, dropColumn: any) {
    event.preventDefault();
    if (this.draggedColumn) {
      let dragIndex = DomHandler.indexWithinGroup(this.draggedColumn, 'preorderablecolumn');
      let dropIndex = DomHandler.indexWithinGroup(dropColumn, 'preorderablecolumn');
      let allowDrop = dragIndex != dropIndex;
      if (
        allowDrop &&
        ((dropIndex - dragIndex == 1 && this.dropPosition === -1) ||
          (dragIndex - dropIndex == 1 && this.dropPosition === 1))
      ) {
        allowDrop = false;
      }

      if (allowDrop && dropIndex < dragIndex && this.dropPosition === 1) {
        dropIndex = dropIndex + 1;
      }

      if (allowDrop && dropIndex > dragIndex && this.dropPosition === -1) {
        dropIndex = dropIndex - 1;
      }

      if (allowDrop) {
        ObjectUtils.reorderArray(<any[]>this.columns, dragIndex, dropIndex);

        this.onColReorder.emit({
          dragIndex: dragIndex,
          dropIndex: dropIndex,
          columns: this.columns,
        });

        if (this.isStateful()) {
          this.zone.runOutsideAngular(() => {
            setTimeout(() => {
              this.saveState();
            });
          });
        }

        if (this.resizableColumns) {
          if (this.columnResizeMode === 'expand') {
            // @ts-ignore
            ObjectUtils.reorderArray(this._initialColWidths, dragIndex, dropIndex);
          }

          // @ts-ignore
          let currentWidth: number[] = this._totalTableWidth();
          ObjectUtils.reorderArray(currentWidth, dragIndex, dropIndex);
          TableOverrides.forceSetTableWidthsInHtml.bind(this)(currentWidth);
        }
      }

      (<ElementRef>this.reorderIndicatorUpViewChild).nativeElement.style.display = 'none';
      (<ElementRef>this.reorderIndicatorDownViewChild).nativeElement.style.display = 'none';
      this.draggedColumn.draggable = false;
      this.draggedColumn = null;
      this.dropPosition = null;
    }
  }

  static forceSetTableWidthsInHtml(this: Table, width: number[]) {
    this.destroyStyleElement();
    this.createStyleElement();

    let innerHTML = '';
    width.forEach((width, index) => {
      let style = `width: ${width}px !important; max-width: ${width}px !important;`;
      innerHTML += `
                #${this.id}-table > .p-datatable-thead > tr > th:nth-child(${index + 1}),
                #${this.id}-table > .p-datatable-tbody > tr > td:nth-child(${index + 1}),
                #${this.id}-table > .p-datatable-tfoot > tr > td:nth-child(${index + 1}) {
                    ${style}
                }
            `;
    });
    this.renderer.setProperty(this.styleElement, 'innerHTML', innerHTML);
  }

  static onColumnDragLeave(this: Table, event: Event) {
    if (this.reorderableColumns && this.draggedColumn) {
      event.preventDefault();

      if ((event.target as HTMLElement).classList.contains('cell-content')) {
        (<ElementRef>this.reorderIndicatorUpViewChild).nativeElement.style.display = 'none';
        (<ElementRef>this.reorderIndicatorDownViewChild).nativeElement.style.display = 'none';
      }
    }
  }

  static _filter(getColumnByField: (field: string) => Column) {
    return function (this: Table) {
      if (!this.restoringFilter) {
        this.first = 0;
        this.firstChange.emit(this.first);
      }

      if (this.lazy) {
        this.onLazyLoad.emit(this.createLazyLoadMetadata());
      } else {
        if (!this.value) {
          return;
        }
        if (!this.hasFilter()) {
          this.filteredValue = null;
          if (this.paginator) {
            this.totalRecords =
              this._totalRecords === 0 && this.value ? this.value.length : this._totalRecords;
          }
        } else {
          let globalFilterFieldsArray;
          if (this.filters['global']) {
            if (!this.columns && !this.globalFilterFields)
              throw new Error(
                'Global filtering requires dynamic columns or globalFilterFields to be defined.',
              );
            else globalFilterFieldsArray = this.globalFilterFields || this.columns;
          }

          this.filteredValue = [];

          for (let i = 0; i < this.value.length; i++) {
            let localMatch = true;
            let globalMatch = false;
            let localFiltered = false;

            for (let prop in this.filters) {
              if (this.filters.hasOwnProperty(prop) && prop !== 'global') {
                localFiltered = true;
                let filterField = prop;
                let filterMeta = this.filters[filterField];
                const column = getColumnByField(filterField);

                if (Array.isArray(filterMeta)) {
                  for (let meta of filterMeta) {
                    localMatch = this.filterService.filters[
                      meta.matchMode || FilterMatchMode.EQUALS
                    ](
                      ColumnUtils.getColumnValue(this.value[i], column),
                      meta.value,
                      this.filterLocale,
                    );

                    if (
                      (meta.operator === FilterOperator.OR && localMatch) ||
                      (meta.operator === FilterOperator.AND && !localMatch)
                    ) {
                      break;
                    }
                  }
                } else {
                  localMatch = this.filterService.filters[
                    filterMeta.matchMode || FilterMatchMode.EQUALS
                  ](
                    ColumnUtils.getColumnValue(this.value[i], column),
                    filterMeta.value,
                    this.filterLocale,
                  );
                }

                if (!localMatch) {
                  break;
                }
              }
            }

            if (this.filters['global'] && !globalMatch && globalFilterFieldsArray) {
              for (let j = 0; j < globalFilterFieldsArray.length; j++) {
                const globalFilterField =
                  globalFilterFieldsArray[j].field || globalFilterFieldsArray[j];
                const column = getColumnByField(globalFilterField);
                globalMatch = (<any>this.filterService).filters[
                  (<any>this.filters['global']).matchMode
                ](
                  ColumnUtils.getColumnValueFormatted(this.value[i], column),
                  (<FilterMetadata>this.filters['global']).value,
                  this.filterLocale,
                );

                if (globalMatch) {
                  break;
                }
              }
            }

            let matches: boolean;
            if (this.filters['global']) {
              matches = localFiltered ? localFiltered && localMatch && globalMatch : globalMatch;
            } else {
              matches = localFiltered && localMatch;
            }

            if (matches) {
              this.filteredValue.push(this.value[i]);
            }
          }

          if (this.filteredValue.length === this.value.length) {
            this.filteredValue = null;
          }

          if (this.paginator) {
            this.totalRecords = this.filteredValue
              ? this.filteredValue.length
              : this._totalRecords === 0 && this.value
                ? this.value.length
                : (this._totalRecords ?? 0);
          }
        }
      }

      this.onFilter.emit({
        filters: <{ [s: string]: FilterMetadata | undefined }>this.filters,
        filteredValue: this.filteredValue || this.value,
      });

      this.tableService.onValueChange(this.value);

      if (this.isStateful() && !this.restoringFilter) {
        this.saveState();
      }

      if (this.restoringFilter) {
        this.restoringFilter = false;
      }

      this.cd.markForCheck();

      if (this.scrollable) {
        this.resetScrollTop();
      }
    };
  }
}
