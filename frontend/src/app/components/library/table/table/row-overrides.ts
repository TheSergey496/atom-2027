import { SelectableRow } from '@openng/optimus-ui/table';
import { DomHandler } from '@openng/optimus-ui/dom';

export class RowOverrides {
  static selectableRowOnArrowDownKey(this: SelectableRow, event: KeyboardEvent) {
    if (!this.isEnabled()) {
      return;
    }

    const row = <HTMLTableRowElement>event.currentTarget;
    const nextRow = this.findNextSelectableRow(row);

    if (nextRow) {
      nextRow.focus();
      nextRow.dispatchEvent(
        new MouseEvent('click', {
          bubbles: true,
          cancelable: true,
          shiftKey: event.shiftKey,
        }),
      );
    }

    event.preventDefault();
  }

  static findPrevSelectableRow(
    this: SelectableRow,
    row: HTMLTableRowElement,
  ): HTMLTableRowElement | null {
    let prevRow = <HTMLTableRowElement>row.previousElementSibling;
    if (prevRow) {
      if (prevRow.getAttribute('data-p-selectable-row')) return prevRow;
      else return this.findPrevSelectableRow(prevRow);
    } else {
      return null;
    }
  }

  static findNextSelectableRow(
    this: SelectableRow,
    row: HTMLTableRowElement,
  ): HTMLTableRowElement | null {
    let nextRow = <HTMLTableRowElement>row.nextElementSibling;

    if (nextRow) {
      if (nextRow.getAttribute('data-p-selectable-row')) return nextRow;
      else return this.findNextSelectableRow(nextRow);
    } else {
      return null;
    }
  }

  static selectableRowOnArrowUpKey(this: SelectableRow, event: KeyboardEvent) {
    if (!this.isEnabled()) {
      return;
    }

    const row = <HTMLTableRowElement>event.currentTarget;
    const prevRow = this.findPrevSelectableRow(row);

    if (prevRow) {
      prevRow.focus();
      prevRow.dispatchEvent(
        new MouseEvent('click', {
          bubbles: true,
          cancelable: true,
          shiftKey: event.shiftKey,
        }),
      );
    }

    event.preventDefault();
  }

  static selectableRowOnEnterKey(this: SelectableRow, event: KeyboardEvent) {
    if (!this.isEnabled()) {
      return;
    }

    this.dataTable.handleRowClick({
      originalEvent: event,
      rowData: this.data,
      rowIndex: this.index,
    });
  }

  static selectableRowOnEndKey(this: SelectableRow, event: KeyboardEvent) {
    if (event.shiftKey) {
      const data = this.dataTable.dataToRender(this.dataTable.processedData);
      const lastRowIndex = data.length - 1;

      DomHandler.clearSelection();
      if (this.dataTable.rangeRowIndex != null) {
        this.dataTable.clearSelectionRange(event);
      }

      this.dataTable.rangeRowIndex = lastRowIndex;
      this.dataTable.selectRange(event, lastRowIndex);
    }
    event.preventDefault();
  }

  static selectableRowOnHomeKey(this: SelectableRow, event: KeyboardEvent) {
    if (event.shiftKey) {
      DomHandler.clearSelection();
      if (this.dataTable.rangeRowIndex != null) {
        this.dataTable.clearSelectionRange(event);
      }

      this.dataTable.rangeRowIndex = 0;
      this.dataTable.selectRange(event, 0);
    }
    event.preventDefault();
  }

  static selectableRowOnSpaceKey(
    clearSelection: () => void,
  ): (this: SelectableRow, event: KeyboardEvent) => void {
    return function (this, event) {
      const isInput =
        event.target instanceof HTMLInputElement ||
        event.target instanceof HTMLSelectElement ||
        event.target instanceof HTMLTextAreaElement;
      if (isInput) {
        return;
      } else {
        clearSelection();
        this.el.nativeElement.blur();
      }

      event.preventDefault();
    };
  }
}
