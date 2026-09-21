import {
  ApplicationRef,
  Component,
  ComponentRef,
  createComponent,
  effect,
  EnvironmentInjector,
  inject,
  OnDestroy,
  OnInit,
  Renderer2,
  signal,
  viewChild,
} from '@angular/core';
import { Dialog } from '@openng/optimus-ui/dialog';
import { Subject, Subscription } from 'rxjs';
import { ProgressSpinner } from '@openng/optimus-ui/progressspinner';

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
export abstract class DialogComponent<Res, Callbacks extends string> implements OnInit, OnDestroy {
  private readonly dialogRef = viewChild(Dialog);

  private onHide = new Subject<void>();
  private _visible = signal(true);
  private _loading = signal(false);
  private _res?: Res = undefined;

  private readonly environmentInjector = inject(EnvironmentInjector);
  private readonly applicationRef = inject(ApplicationRef);
  private readonly renderer2 = inject(Renderer2);

  protected _callbacks = {} as Record<Callbacks, CallbackFunction<Res>> &
    Partial<Record<'cancel', CallbackFunction<Res>>>;

  public get visible() {
    return this._visible();
  }

  public set visible(value: boolean) {
    const valueBefore = this.visible;
    this._visible.set(value);
    if (!value && valueBefore) {
      void this._callbacks.cancel?.call(this, this, this._res);
      this.onHide.next();
    }
  }

  public get loading() {
    return this._loading();
  }

  public set loading(value: boolean) {
    this._loading.set(value);
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
    effect(() => {
      const dialogRef = this.dialogRef();
      if (dialogRef != null) {
        dialogRef.visible = this.visible;
        dialogRef.cd.detectChanges();
      }
    });
    let loaderComponentRef: ComponentRef<DialogLoader> | undefined = undefined;
    const getDialogNativeEl = (dialogRef: Dialog): unknown => {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-call,@typescript-eslint/no-unsafe-member-access
      return dialogRef.el.nativeElement.querySelector('.p-component.p-dialog');
    };
    effect(() => {
      const dialogRef = this.dialogRef();
      if (dialogRef != null) {
        if (this.loading) {
          loaderComponentRef = createComponent(DialogLoader, {
            environmentInjector: this.environmentInjector,
          });
          this.applicationRef.attachView(loaderComponentRef.hostView);
          const dialog = getDialogNativeEl(dialogRef);
          this.renderer2.appendChild(dialog, loaderComponentRef.location.nativeElement);
          this.renderer2.addClass(dialog, 'disabled');
        } else if (loaderComponentRef != null) {
          loaderComponentRef.destroy();
          const dialog = getDialogNativeEl(dialogRef);
          this.renderer2.removeClass(dialog, 'disabled');
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
    this.onHide.unsubscribe();
  }

  public subscribeOnHide(callback: () => void): Subscription {
    return this.onHide.subscribe(callback);
  }
}

@Component({
  template: ` <div class="disabled">
    <p-progressSpinner [style]="{ width: '50px', height: '50px' }"></p-progressSpinner>
  </div>`,
  imports: [ProgressSpinner],
  styles: `
    .disabled {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 9999999;
    }
  `,
})
// eslint-disable-next-line @typescript-eslint/no-extraneous-class
class DialogLoader {}
