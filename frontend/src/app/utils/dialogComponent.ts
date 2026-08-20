import { Component, effect, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
import { Dialog } from '@openng/optimus-ui/dialog';
import { Subject, Subscription } from 'rxjs';

@Component({ template: '' })
export abstract class DialogComponent<Res> implements OnInit, OnDestroy {
  private readonly result = new Subject<Res | undefined>();
  private readonly dialogRef = viewChild(Dialog);
  private _visible = signal(true);
  private _res?: Res = undefined;

  public set res(value: Res | undefined | null) {
    this._res = value ?? undefined;
  }

  public get visible() {
    return this._visible();
  }

  public set visible(value: boolean) {
    this._visible.set(value);
  }

  protected constructor() {
    effect(() => {
      const dialogRef = this.dialogRef();
      if (dialogRef != null) {
        dialogRef.visible = this.visible;
        dialogRef.cd.detectChanges();
        if (!this.visible) {
          this.result.next(this._res);
        }
      }
    });
  }

  ngOnInit(): void {
    const dialogRef = this.dialogRef();
    if (dialogRef != null) {
      dialogRef.visibleChange.subscribe((v) => {
        this.visible = v;
      });
    }
  }

  ngOnDestroy(): void {
    this.result.unsubscribe();
  }

  public subscribeToResult(callback: (res: Res | undefined) => void): Subscription {
    return this.result.subscribe(callback);
  }
}
