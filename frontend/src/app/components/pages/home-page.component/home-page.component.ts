import { Component, inject } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { DummyDialogComponent } from '../../forms/dummy-dialog.component/dummy-dialog.component';
import { DialogService } from '../../../services/dialog.service';

@Component({
  selector: 'pro-legacy-home-page.component',
  imports: [Button],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css',
})
export class HomePageComponent {
  private readonly dialogService = inject(DialogService);

  onClick() {
    const dialog = this.dialogService.createDialog(DummyDialogComponent);
    dialog.callbacks = {
      ok: async (d, r) => {
        d.loading = true;
        await new Promise<void>((resolve) =>
          setTimeout(() => {
            resolve();
          }, 1000),
        );
        console.log(r);
        d.visible = false;
      },
      enableLoading: {
        async callback(dialog) {
          await dialog.loadingService.withLoader(async () => {
            await new Promise<void>((resolve) =>
              setTimeout(() => {
                dialog.loading = false;
                resolve();
              }, 1500),
            );
          });
        },
      },
    };
  }
}
