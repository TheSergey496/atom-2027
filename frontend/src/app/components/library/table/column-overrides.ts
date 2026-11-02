import { SimpleChanges } from '@angular/core';
import { ReorderableColumn } from '@openng/optimus-ui/table';

export class ColumnOverrides {
  public static onReorderableColumnChanges(this: ReorderableColumn, changes: SimpleChanges) {
    const disabledChange = changes['pReorderableColumnDisabled'];
    if (disabledChange && !!disabledChange.currentValue !== !!disabledChange.previousValue) {
      this.unbindEvents();
      if(this.el?.nativeElement != null) {
        this.el.nativeElement.draggable = false;
      }
      if (!disabledChange.currentValue) {
        this.bindEvents();
      }
    }
  }
}
