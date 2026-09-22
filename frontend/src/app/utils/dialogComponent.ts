import { Component, effect, inject, Injector, OnDestroy, viewChild } from '@angular/core';
import { Dialog } from '@openng/optimus-ui/dialog';
import { Subject, Subscription } from 'rxjs';
import { LoadingService } from '../services/loading.service';

export type DialogCallback<Res> = CallbackFunction<Res> | CallbackClass<Res>;
export type CallbackFunction<Res> = (
  dialog: DialogComponent<Res, string>,
  res: Res | undefined,
) => void | Promise<void>;
export interface CallbackClass<Res> {
  callback(dialog: DialogComponent<Res, string>, res: Res | undefined): void | Promise<void>;
}

@Component({
  template: '',
})
export abstract class DialogComponent<Res, Callbacks extends string> implements OnDestroy {
  private readonly dialogRef = viewChild(Dialog);

  public loadingService?: LoadingService;

  private readonly onHide = new Subject<void>();

  private _visible = true;
  private _res?: Res = undefined;

  protected _callbacks = {} as Record<Callbacks, CallbackFunction<Res>> &
    Partial<Record<'cancel', CallbackFunction<Res>>>;

  public get visible() {
    return this._visible;
  }

  public set visible(value: boolean) {
    const valueBefore = this.visible;
    this._visible = value;
    if (!value && valueBefore) {
      void this._callbacks.cancel?.call(this, this, this._res);
      this.onHide.next();
    }
  }

  public get loading() {
    return this.loadingService?.loading ?? false;
  }

  public set loading(value: boolean) {
    if (value) {
      this.loadingService?.beginLoading();
    } else {
      this.loadingService?.stopLoading();
    }
  }

  public set callbacks(value: Record<Callbacks, DialogCallback<Res>>) {
    function isCallbackClass(cb: DialogCallback<Res>): cb is CallbackClass<Res> {
      return typeof cb === 'object' && typeof cb.callback === 'function';
    }

    const target = this._callbacks as Record<Callbacks, CallbackFunction<Res>>;

    (Object.entries(value) as [Callbacks, DialogCallback<Res>][]).forEach(([key, cb]) => {
      target[key] = isCallbackClass(cb) ? cb.callback.bind(cb) : cb;
    });
  }

  protected set res(value: Res | undefined | null) {
    this._res = value ?? undefined;
  }

  protected get res(): Res | undefined {
    return this._res;
  }

  protected constructor() {
    const injector = inject(Injector);

    const getDialogNativeEl = (dialogRef: Dialog): unknown => {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-call,@typescript-eslint/no-unsafe-member-access
      return dialogRef.el.nativeElement.querySelector('.p-component.p-dialog');
    };

    effect(() => {
      const dialogRef = this.dialogRef();
      if (dialogRef != null) {
        dialogRef.visibleChange.subscribe((v) => {
          this.visible = v;
        });

        dialogRef.visible = this.visible;
        dialogRef.cd.detectChanges();

        this.loadingService?.stopLoadingImmediate();
        this.loadingService = new LoadingService(getDialogNativeEl(dialogRef), injector);
      }
    });
  }

  ngOnDestroy(): void {
    this.onHide.unsubscribe();
  }

  public subscribeOnHide(callback: () => void): Subscription {
    return this.onHide.subscribe(callback);
  }
}
