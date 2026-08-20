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
    dialog.subscribeToResult((value) => {
      console.log(value);
    });
  }
}
