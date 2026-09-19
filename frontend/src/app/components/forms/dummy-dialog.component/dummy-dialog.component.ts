import { Component } from '@angular/core';
import { Dialog } from '@openng/optimus-ui/dialog';
import { DialogCallback, DialogComponent } from '../../../utils/dialogComponent';
import { Button } from '@openng/optimus-ui/button';

@Component({
  imports: [Dialog, Button],
  selector: 'pro-legacy-dummy-dialog.component',
  styleUrl: './dummy-dialog.component.css',
  templateUrl: './dummy-dialog.component.html',
})
export class DummyDialogComponent extends DialogComponent<
  number,
  { ok: DialogCallback<number>; enableLoading: DialogCallback<number> }
> {
  constructor() {
    super();
    this.res = 496;
  }

  protected ok() {
    void this._callbacks.ok(this, this.res);
  }

  protected enableLoading() {
    void this._callbacks.enableLoading(this, this.res);
  }

  protected close() {
    this.visible = false;
  }
}
