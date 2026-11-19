import {
  ChangeDetectorRef,
  Component,
  inject,
  OnDestroy,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { ButtonModule } from '@openng/optimus-ui/button';
import { TableComponent } from '../../library/table/table/table.component';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { ToolbarModule } from '@openng/optimus-ui/toolbar';
import { TooltipModule } from '@openng/optimus-ui/tooltip';
import { Column } from '../../library/table/column';
import { DialogService } from '../../../services/dialog.service';
import { MessageService } from '@openng/optimus-ui/api';
import { SyncConfirmationService } from '../../../services/sync-confirmation.service';
import { GlobalLoadingService } from '../../../services/loading.service';
import { lastValueFrom, Subscription } from 'rxjs';
import { BlockUIModule } from '@openng/optimus-ui/blockui';
import { CheckStatusLocaleEnum } from '../../../enums/CheckStatusLocaleEnum';
import { CheckTypeLocaleEnum } from '../../../enums/CheckTypeLocaleEnum';
import { CheckDto } from '../../../gen/atom2027-dto';
import { CheckService } from '../../../gen/atom2027-controllers';
import { CheckStatusRecord } from '../../../gen/entities-enums';
import { CheckEditDialogComponent } from '../../forms/check-edit-dialog/check-edit-dialog.component';
import { SelectButton } from '@openng/optimus-ui/selectbutton';
import { FormsModule } from '@angular/forms';
import { Checkbox } from '@openng/optimus-ui/checkbox';
import { ResetService } from '../../../services/reset.service';
import { Router } from '@angular/router';

@Component({
  imports: [
    TableComponent,
    ButtonModule,
    ToolbarModule,
    TooltipModule,
    BlockUIModule,
    TableFastSearchComponent,
    SelectButton,
    FormsModule,
    Checkbox,
  ],
  selector: 'pro-legacy-check',
  styleUrl: './check.component.css',
  templateUrl: './check.component.html',
})
export class CheckComponent implements OnInit, OnDestroy {
  protected readonly CheckStatusRecord = CheckStatusRecord;

  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'checkId',
      header: 'Номер проверки',
      width: 120,
    },
    {
      type: 'enum',
      field: 'checkType',
      header: 'Вид проверки',
      decoration: 'tag',
      locale: CheckTypeLocaleEnum,
      width: 350,
    },
    {
      type: 'int',
      field: 'edifice.id',
      header: 'ID объекта',
      width: 280,
    },
    {
      type: 'text',
      field: 'edifice.name',
      header: 'Наименование объекта',
      width: 255,
    },
    {
      type: 'date',
      field: 'planDate',
      decoration: 'invalidDate',
      header: 'Плановая дата проверки',
    },
    {
      type: 'text',
      field: 'responsible.fullName',
      header: 'Ответственный за проведение проверки',
    },
    {
      type: 'enum',
      field: 'checkStatus',
      header: 'Текущее состояние',
      decoration: 'tag',
      locale: CheckStatusLocaleEnum,
      width: 350,
    },
    {
      type: 'boolean',
      field: 'finishedAfterExpiry',
      header: 'Выполнено с опозданием',
      width: 350,
    },
    {
      type: 'text',
      field: 'comment',
      header: 'Комментарий',
      width: 350,
    },
  ]);
  protected readonly getRowId = (row: CheckDto) => `${row?.checkId}`;
  protected readonly filter = signal<boolean>(false);
  protected readonly loading = signal<boolean>(false);
  protected readonly checks = signal<CheckDto[]>([]);
  protected selectedCheck = signal<CheckDto | null>(null);

  private readonly intervalId: any;
  private resetSubscription?: Subscription;

  protected onlyCancelled = false;
  protected onlyCancelledOptions = [
    {
      label: 'Основные',
      value: false,
    },
    {
      label: 'Отменённые',
      value: true,
    },
  ];
  protected onlyUnfinished = false;

  private readonly table = viewChild<TableComponent>('table');
  private readonly checkService = inject(CheckService);
  private readonly dialogService = inject(DialogService);
  private readonly messageService = inject(MessageService);
  private readonly syncConfirmationService = inject(SyncConfirmationService);
  private readonly globalLoadingService = inject(GlobalLoadingService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly resetService = inject(ResetService);
  private readonly router = inject(Router);

  constructor() {
    this.intervalId = setInterval(() => this.table()?.table()?.cd?.detectChanges(), 3000);
  }

  ngOnInit() {
    this.loadData().then();
    this.resetSubscription = this.resetService.subscribe(async () => {
      await this.loadData();
    });
    let checkId: unknown = this.router.lastSuccessfulNavigation()?.extras.state?.['checkId'];
    if (checkId != null) {
      requestAnimationFrame(() => {
        this.table()?.findAndSelectRow({ checkId: checkId }).then();
      });
    }
  }

  ngOnDestroy() {
    clearInterval(this.intervalId);
    this.resetSubscription?.unsubscribe();
  }

  protected async loadData() {
    this.loading.set(true);
    this.checks.set([]);
    try {
      const checks = await lastValueFrom(
        this.checkService.getChecks(this.onlyCancelled, this.onlyUnfinished),
      );
      this.checks.set(checks);
      this.cdr.detectChanges();
      requestAnimationFrame(() => {
        this.cdr.detectChanges();
        window.dispatchEvent(new Event('resize'));
      });
    } finally {
      this.loading.set(false);
    }
  }

  protected async createCheck() {
    const dialog = this.dialogService.createDialog(CheckEditDialogComponent);
    await dialog.init();
    dialog.callbacks = {
      save: async (dialog, check) => {
        dialog.loading = true;
        try {
          const newCheck = await lastValueFrom(this.checkService.createCheck(check!));
          this.messageService.add({
            severity: 'success',
            summary: 'Выполнено',
            detail: 'Проверка назначена',
          });
          await this.loadData();
          await this.table()?.findAndSelectRow(newCheck);

          dialog.visible = false;
        } finally {
          dialog.loading = false;
        }
      },
    };
  }

  protected async updateCheck() {
    const checkId = this.selectedCheck()?.checkId;
    if (checkId == null) {
      return;
    }

    const dialog = this.dialogService.createDialog(CheckEditDialogComponent);
    await dialog.init(checkId);
    dialog.callbacks = {
      save: async (dialog, check) => {
        dialog.loading = true;
        try {
          const updatedCheck = await lastValueFrom(this.checkService.updateCheck(check!));
          this.messageService.add({
            severity: 'success',
            summary: 'Выполнено',
            detail: 'Данные проверки сохранены',
          });
          this.table()?.updateRows([updatedCheck]);
          this.selectedCheck.set(updatedCheck);
          dialog.visible = false;
        } finally {
          dialog.loading = false;
        }
      },
    };
  }

  protected async cancelCheck() {
    const checkId = this.selectedCheck()?.checkId;
    if (checkId == null) {
      return;
    }

    const b = await this.syncConfirmationService.confirm({
      message: 'Вы уверены, что хотите отменить выбранную проверку?',
    });

    if (!b) {
      return;
    }

    await this.globalLoadingService.withLoader(async () => {
      const canceledCheck = await lastValueFrom(this.checkService.cancelCheck(checkId));
      this.messageService.add({
        severity: 'success',
        summary: 'Выполнено',
        detail: 'Проверка отменена',
      });

      this.table()?.removeRows([canceledCheck], 'Проверка отменена');
      this.selectedCheck.set(canceledCheck);
    });
  }

  protected onOnlyCancelledChange(newValue: boolean) {
    this.onlyCancelled = newValue;
    this.loadData().then();
  }

  protected onOnlyUnfinishedChanged(newValue: boolean) {
    this.onlyUnfinished = newValue;
    this.loadData().then();
  }
}
