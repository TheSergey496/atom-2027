import { TTRow, TTSelectableRow } from '@openng/optimus-ui/treetable';

export class RowOverrides {
  public static onArrowDownKey(this: TTRow, event: KeyboardEvent) {
    let nextRow = this.el?.nativeElement?.nextElementSibling;
    if (nextRow) {
      this.focusRowChange(<HTMLElement>event.currentTarget, nextRow);
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

  public static onArrowUpKey(this: TTRow, event: KeyboardEvent) {
    let prevRow = this.el?.nativeElement?.previousElementSibling;
    if (prevRow) {
      this.focusRowChange(<HTMLElement>event.currentTarget, prevRow);
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
}