import {
  ApplicationRef,
  Component,
  ComponentRef,
  createComponent,
  effect,
  EnvironmentInjector,
  Injector,
  Renderer2,
  Signal,
  signal,
} from '@angular/core';
import { ProgressSpinner } from '@openng/optimus-ui/progressspinner';

export class LoadingService {
  private targetNativeElement?: unknown;
  private readonly environmentInjector;
  private readonly applicationRef;
  private readonly renderer2;

  private loaderComponentRef?: ComponentRef<Loader>;
  private _counter = 0;

  constructor(targetNativeElement: Signal<unknown>, injector: Injector) {
    this.environmentInjector = injector.get(EnvironmentInjector);
    this.applicationRef = injector.get(ApplicationRef);
    this.renderer2 = injector.get(Renderer2);

    effect(() => {
      this.hideLoader();
      const targetNativeElementBefore = this.targetNativeElement;
      this.targetNativeElement = targetNativeElement();
      if (targetNativeElementBefore == this.targetNativeElement) {
        return;
      }
      if (this.loaderComponentRef == null) {
        this.loaderComponentRef = createComponent(Loader, {
          environmentInjector: this.environmentInjector,
        });
        this.applicationRef.attachView(this.loaderComponentRef.hostView);
      }
      if (targetNativeElementBefore != null) {
        this.renderer2.removeChild(
          targetNativeElementBefore,
          this.loaderComponentRef.location.nativeElement,
        );
      }
      if (this.targetNativeElement != null) {
        this.renderer2.appendChild(
          this.targetNativeElement,
          this.loaderComponentRef.location.nativeElement,
        );
      }
      if (this.loading) {
        this.showLoader();
      }
    });
  }

  public get loading(): boolean {
    return this._counter > 0;
  }

  public beginLoading() {
    this.counter += 1;
  }

  public stopLoading() {
    if (this._counter === 0) {
      return;
    }
    this.counter -= 1;
  }

  public async withLoader(f: () => void | Promise<void>) {
    this.beginLoading();
    try {
      await f();
    } finally {
      this.stopLoading();
    }
  }

  public stopLoadingImmediate() {
    this._counter = 0;
  }

  private get counter(): number {
    return this._counter;
  }

  private set counter(value: number) {
    if (this._counter === 0 && value > 0) {
      this.showLoader();
    } else if (this._counter > 0 && value === 0) {
      this.hideLoader();
    }
    this._counter = value;
  }

  private showLoader() {
    if (this.loaderComponentRef == null) return;

    this.loaderComponentRef.instance.visible = true;
    this.renderer2.addClass(this.targetNativeElement, 'disabled');
  }

  private hideLoader() {
    if (this.loaderComponentRef == null) return;

    this.loaderComponentRef.instance.visible = false;
    this.renderer2.removeClass(this.targetNativeElement, 'disabled');
  }
}

@Component({
  template: ` @if (_visible()) {
    <div class="loader">
      <p-progressSpinner [style]="{ width: '75px', height: '75px' }"></p-progressSpinner>
    </div>
  }`,
  imports: [ProgressSpinner],
  styles: `
    .loader {
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
class Loader {
  protected readonly _visible = signal(false);

  public set visible(value: boolean) {
    this._visible.set(value);
  }
}
