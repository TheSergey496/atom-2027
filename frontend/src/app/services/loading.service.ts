import {
  ApplicationRef,
  Component,
  ComponentRef,
  createComponent,
  EnvironmentInjector,
  Injector,
  Renderer2,
  signal,
} from '@angular/core';
import { ProgressSpinner } from '@openng/optimus-ui/progressspinner';

export class LoadingService {
  private readonly targetNativeElement: unknown;
  private readonly environmentInjector;
  private readonly applicationRef;
  private readonly renderer2;

  private readonly loaderComponentRef: ComponentRef<Loader>;
  private _counter = 0;

  constructor(targetNativeElement: unknown, injector: Injector) {
    this.environmentInjector = injector.get(EnvironmentInjector);
    this.applicationRef = injector.get(ApplicationRef);
    this.renderer2 = injector.get(Renderer2);

    this.targetNativeElement = targetNativeElement;
    this.loaderComponentRef = createComponent(Loader, {
      environmentInjector: this.environmentInjector,
    });
    this.applicationRef.attachView(this.loaderComponentRef.hostView);
    this.renderer2.appendChild(
      this.targetNativeElement,
      this.loaderComponentRef.location.nativeElement,
    );
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
      this.renderer2.addClass(this.targetNativeElement, 'disabled');
    } else if (this._counter > 0 && value === 0) {
      this.hideLoader();
      this.renderer2.removeClass(this.targetNativeElement, 'disabled');
    }
    this._counter = value;
  }

  private showLoader() {
    this.loaderComponentRef.instance.visible = true;
  }

  private hideLoader() {
    this.loaderComponentRef.instance.visible = false;
  }
}

@Component({
  template: ` @if (_visible()) {
    <div class="loader">
      <p-progressSpinner [style]="{ width: '50px', height: '50px' }"></p-progressSpinner>
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
