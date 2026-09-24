import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  Injector,
  OnDestroy,
  signal,
  viewChild,
} from '@angular/core';
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
export abstract class DialogComponent<Res, Callbacks extends string>
  implements AfterViewInit, OnDestroy
{
  private readonly dialogRef = viewChild.required(Dialog);

  public loadingService: LoadingService;

  private readonly onHide = new Subject<void>();

  private isInitialized = signal(false);
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
    return this.loadingService.loading;
  }

  public set loading(value: boolean) {
    if (value) {
      this.loadingService.beginLoading();
    } else {
      this.loadingService.stopLoading();
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
    this.loadingService = new LoadingService(
      computed(() => {
        const dialogRef = this.dialogRef();
        if (!this.isInitialized()) {
          return null;
        }
        // eslint-disable-next-line @typescript-eslint/no-unsafe-return,@typescript-eslint/no-unsafe-call,@typescript-eslint/no-unsafe-member-access
        return dialogRef.el.nativeElement.querySelector('.p-component.p-dialog');
      }),
      inject(Injector),
    );

    effect(() => {
      const dialogRef = this.dialogRef();
      dialogRef.visibleChange.subscribe((v) => {
        this.visible = v;
      });

      dialogRef.visible = this.visible;
      dialogRef.cd.detectChanges();
    });
  }

  ngAfterViewInit(): void {
    this.isInitialized.set(true);
  }

  ngOnDestroy(): void {
    this.onHide.unsubscribe();
  }

  public subscribeOnHide(callback: () => void): Subscription {
    return this.onHide.subscribe(callback);
  }
}
