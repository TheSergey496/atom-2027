import { TreeTable } from '@openng/optimus-ui/treetable';
import { findSingle, getIndex } from '@openng/optimus-ui-utils';
import { DomHandler } from '@openng/optimus-ui/dom';
import { ElementRef } from '@angular/core';

export class TreeTableOverrides {
  static forceSetTableWidthsInHtml(this: TreeTable, width: number[]) {
    const columns = TreeTableOverrides.getHeaders(this.el.nativeElement);
    const scrollableViews: { el: any; width: number }[] = [];

    for (let i = 0; i < columns.length; i++) {
      const column = columns[i];
      const columnWidth = width[i];
      const scrollableView = this.findParentScrollableView(column);
      let cachedScrollableView = scrollableViews.find(v => v.el === scrollableView);
      if (cachedScrollableView == null) {
        cachedScrollableView = { el: scrollableView, width: 0 };
        scrollableViews.push(cachedScrollableView);
      }

      cachedScrollableView.width += columnWidth;

      let scrollableBodyTable =
        <any>findSingle(scrollableView, '[data-pc-section="scrollablebody"] table') ||
        findSingle(scrollableView, '[data-pc-name="virtualscroller"] table');
      let scrollableHeaderTable = <any>(
        findSingle(scrollableView, '[data-pc-section="scrollableheadertable"]')
      );
      let scrollableFooterTable = <any>(
        findSingle(scrollableView, '[data-pc-section="scrollablefootertable"]')
      );
      let resizeColumnIndex = getIndex(column);

      this.resizeColGroup(scrollableHeaderTable, resizeColumnIndex, columnWidth, null);
      this.resizeColGroup(scrollableBodyTable, resizeColumnIndex, columnWidth, null);
      this.resizeColGroup(scrollableFooterTable, resizeColumnIndex, columnWidth, null);
    }
  }

  private static getHeaders(tableEl: any) {
    const tableHead = DomHandler.findSingle(
      tableEl,
      '.p-treetable-unfrozen-view[data-pc-section="scrollableview"]',
    );
    if (tableHead == null) {
      return [];
    }
    const headers = DomHandler.find(tableHead, 'tr > th[ttreorderablecolumn]');
    return headers ?? [];
  }

  public static onColumnResizeEnd(this: TreeTable, event: MouseEvent, column: any) {
    let delta =
      (<ElementRef>this.resizeHelperViewChild).nativeElement.offsetLeft -
      <number>this.lastResizerHelperX;
    let columnWidth = column.offsetWidth;
    let newColumnWidth = columnWidth + delta;
    let minWidth = column.style.minWidth || 15;

    if (columnWidth + delta > parseInt(minWidth)) {
      if (this.columnResizeMode === 'fit') {
        let nextColumn = column.nextElementSibling;
        while (!nextColumn.offsetParent) {
          nextColumn = nextColumn.nextElementSibling;
        }

        if (nextColumn) {
          let nextColumnWidth = nextColumn.offsetWidth - delta;
          let nextColumnMinWidth = nextColumn.style.minWidth || 15;

          if (newColumnWidth > 15 && nextColumnWidth > parseInt(nextColumnMinWidth)) {
            if (this.scrollable) {
              let scrollableView = this.findParentScrollableView(column);
              let scrollableBodyTable =
                <any>findSingle(scrollableView, '[data-pc-section="scrollablebody"] table') ||
                findSingle(scrollableView, '[data-pc-name="virtualscroller"] table');
              let scrollableHeaderTable = <any>(
                findSingle(scrollableView, '[data-pc-section="scrollableheadertable"]')
              );
              let scrollableFooterTable = <any>(
                findSingle(scrollableView, '[data-pc-section="scrollablefootertable"]')
              );
              let resizeColumnIndex = getIndex(column);

              this.resizeColGroup(
                scrollableHeaderTable,
                resizeColumnIndex,
                newColumnWidth,
                nextColumnWidth,
              );
              this.resizeColGroup(
                scrollableBodyTable,
                resizeColumnIndex,
                newColumnWidth,
                nextColumnWidth,
              );
              this.resizeColGroup(
                scrollableFooterTable,
                resizeColumnIndex,
                newColumnWidth,
                nextColumnWidth,
              );
            } else {
              column.style.width = newColumnWidth + 'px';
              if (nextColumn) {
                nextColumn.style.width = nextColumnWidth + 'px';
              }
            }
          }
        }
      } else if (this.columnResizeMode === 'expand') {
        if (this.scrollable) {
          let scrollableView = this.findParentScrollableView(column);
          let scrollableBodyTable =
            <any>findSingle(scrollableView, '[data-pc-section="scrollablebody"] table') ||
            findSingle(scrollableView, '[data-pc-name="virtualscroller"] table');
          let scrollableHeaderTable = <any>(
            findSingle(scrollableView, '[data-pc-section="scrollableheadertable"]')
          );
          let scrollableFooterTable = <any>(
            findSingle(scrollableView, '[data-pc-section="scrollablefootertable"]')
          );
          let resizeColumnIndex = getIndex(column);

          this.resizeColGroup(scrollableHeaderTable, resizeColumnIndex, newColumnWidth, null);
          this.resizeColGroup(scrollableBodyTable, resizeColumnIndex, newColumnWidth, null);
          this.resizeColGroup(scrollableFooterTable, resizeColumnIndex, newColumnWidth, null);
        } else {
          (<ElementRef>this.tableViewChild).nativeElement.style.width =
            this.tableViewChild?.nativeElement.offsetWidth + delta + 'px';
          column.style.width = newColumnWidth + 'px';
          let containerWidth = this.tableViewChild?.nativeElement.style.width;
          (<ElementRef>this.el).nativeElement.style.width = containerWidth + 'px';
        }
      }

      this.onColResize.emit({
        element: column,
        delta: delta,
      });

      (this.resizeHelperViewChild as ElementRef).nativeElement.style.display = 'none';

      this.el.nativeElement.removeAttribute('data-p-unselectable-text');
      !this.$unstyled() && (this.el.nativeElement.style['user-select'] = '');
    }
  }
}
