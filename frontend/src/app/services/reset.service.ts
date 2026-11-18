import { inject, Service } from '@angular/core';
import { lastValueFrom, Subject, Subscription } from 'rxjs';
import { EdificeService } from '../gen/atom2027-controllers';
import { GlobalLoadingService } from './loading.service';

@Service()
export class ResetService {
  private readonly _event = new Subject<void>();
  private readonly edificeService = inject(EdificeService);
  private readonly globalLoadingService = inject(GlobalLoadingService);

  public subscribe(f: () => void | Promise<void>): Subscription {
    return this._event.subscribe({ next: f });
  }

  public async resetData() {
    await this.globalLoadingService.withLoader(async () => {
      await lastValueFrom(this.edificeService.resetData());
    });
    this._event.next();
  }
}
