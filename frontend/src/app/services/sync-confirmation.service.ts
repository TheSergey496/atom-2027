import { inject, Service } from '@angular/core';
import { Confirmation, ConfirmationService } from '@openng/optimus-ui/api';

@Service()
export class SyncConfirmationService {
  private readonly confirmationService = inject(ConfirmationService);

  public confirm(confirm: Omit<Confirmation, 'accept' | 'reject'>): Promise<boolean> {
    return new Promise(
      (resolve) => {
        this.confirmationService.confirm(
          {
            ...confirm,
            key: confirm?.key ?? 'confirm',
            accept: () => resolve(true),
            reject: () => resolve(false)
          }
        )
      }
    )
  }
}
