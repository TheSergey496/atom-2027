import { Component, inject } from '@angular/core';
import { DummyDialogComponent } from '../../forms/dummy-dialog.component/dummy-dialog.component';
import { DialogService } from '../../../services/dialog.service';
import { ButtonModule } from '@openng/optimus-ui/button';
import { ExportFormat, ReportService } from '../../../services/report.service';
import { lastValueFrom } from 'rxjs';
import FileSaver from 'file-saver';

@Component({
  selector: 'pro-legacy-home-page.component',
  imports: [ButtonModule],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css',
})
export class HomePageComponent {
  private readonly dialogService = inject(DialogService);
  private readonly reportService = inject(ReportService);

  protected readonly ExportFormat = ExportFormat;

  async testReport(format: ExportFormat) {
    const blob = await lastValueFrom(this.reportService.testReport(format));
    let filename = "Тест";
    switch (format) {
      case ExportFormat.PDF:
        filename += ".pdf"
        break;
      case ExportFormat.XLSX:
        filename += ".xlsx"
        break;
      case ExportFormat.DOCX:
        filename += ".docx"
        break;

    }
    FileSaver.saveAs(blob, filename);
  }

  onClick() {
    const dialog = this.dialogService.createDialog(DummyDialogComponent);
    dialog.callbacks = {
      ok: async (d, r) => {
        d.loading = true;
        await new Promise<void>(resolve =>
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
            await new Promise<void>(resolve =>
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
