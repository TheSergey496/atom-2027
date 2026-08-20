import {
  ApplicationRef,
  createComponent,
  EnvironmentInjector,
  inject,
  Service,
  Type,
} from '@angular/core';
import { DialogComponent } from '../utils/dialogComponent';
import { DOCUMENT } from '@angular/common';

@Service()
export class DialogService {
  private readonly document = inject(DOCUMENT);
  private readonly environmentInjector = inject(EnvironmentInjector);
  private readonly applicationRef = inject(ApplicationRef);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  public createDialog<T extends DialogComponent<any>>(component: Type<T>): T {
    const dialogRef = createComponent(component, { environmentInjector: this.environmentInjector });
    this.applicationRef.attachView(dialogRef.hostView);
    this.document.body.appendChild(dialogRef.location.nativeElement);

    const instance = dialogRef.instance;
    instance.subscribeToResult(() => {
      dialogRef.destroy();
    });

    return instance;
  }
}
