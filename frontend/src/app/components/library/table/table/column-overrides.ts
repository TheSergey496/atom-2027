import { SimpleChanges } from '@angular/core';
import { FrozenColumn, ReorderableColumn } from '@openng/optimus-ui/table';
import { DomHandler } from '@openng/optimus-ui/dom';

export class ColumnOverrides {
  public static onReorderableColumnChanges(this: ReorderableColumn, changes: SimpleChanges) {
    const disabledChange = changes['pReorderableColumnDisabled'];
    if (disabledChange && !!disabledChange.currentValue !== !!disabledChange.previousValue) {
      this.unbindEvents();
      if (this.el?.nativeElement != null) {
        this.el.nativeElement.draggable = false;
      }
      if (!disabledChange.currentValue) {
        this.bindEvents();
      }
    }
  }

  public static frozenColumnUpdateStickyPosition(this: FrozenColumn) {
    if (this._frozen) {
      if (this.alignFrozen === 'right') {
        let right = 0;
        let sibling = this.el.nativeElement.nextElementSibling;
        while (sibling) {
          right += DomHandler.getOuterWidth(sibling);
          sibling = sibling.nextElementSibling;
        }
        this.el.nativeElement.style.right = right + 'px';
        const row = this.el.nativeElement.parentElement?.parentElement;
        const scroller = row?.parentElement?.parentElement;
        if (row != null && scroller != null) {
          const widthDifference = scroller.clientWidth - row.clientWidth;
          // @ts-ignore
          const cachedWidthDifference = this._widthDifference;

          if (cachedWidthDifference !== widthDifference) {
            if (widthDifference > 0) {
              // @ts-ignore
              this._widthDifference = widthDifference;
              this.el.nativeElement.style.transform = `translateX(${widthDifference}px)`;
            } else {
              // @ts-ignore
              this._widthDifference = 0;
              this.el.nativeElement.style.removeProperty('transform');
            }
          }
        }
      } else {
        let left = 0;
        let sibling = this.el.nativeElement.previousElementSibling;
        while (sibling) {
          left += DomHandler.getOuterWidth(sibling);
          sibling = sibling.previousElementSibling;
        }
        this.el.nativeElement.style.left = left + 'px';
      }

      // const filterRow = this.el.nativeElement?.parentElement?.nextElementSibling;
      // if (filterRow) {
      //   let index = DomHandler.index(this.el.nativeElement);
      //   if (filterRow.children && filterRow.children[index]) {
      //     filterRow.children[index].style.left = this.el.nativeElement.style.left;
      //     filterRow.children[index].style.right = this.el.nativeElement.style.right;
      //   }
      // }
    }
  }
}
