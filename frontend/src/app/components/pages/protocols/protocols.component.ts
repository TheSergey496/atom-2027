import {
  ChangeDetectorRef,
  Component,
  inject,
  OnDestroy,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { TableComponent } from '../../library/table/table/table.component';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { Column } from '../../library/table/column';
import { ProtocolDto } from '../../../gen/atom2027-dto';
import { ResetService } from '../../../services/reset.service';
import { ProtocolService } from '../../../gen/atom2027-controllers';
import { lastValueFrom, Subscription } from 'rxjs';
import { DialogService } from '../../../services/dialog.service';
import { ProtocolRegistrationDialogComponent } from '../../forms/protocol-registration-dialog/protocol-registration-dialog.component';
import { MessageService } from '@openng/optimus-ui/api';

@Component({
  imports: [Button, TableComponent, TableFastSearchComponent, Toolbar, Tooltip],
  selector: 'pro-legacy-protocols',
  styleUrl: './protocols.component.css',
  templateUrl: './protocols.component.html',
})
export class ProtocolsComponent implements OnInit, OnDestroy {
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: '№ Протокола',
      width: 120,
    },
    {
      type: 'int',
      field: 'check.checkId',
      header: '№ Проверки',
      width: 120,
    },
    {
      type: 'boolean',
      field: 'isNotPlanned',
      header: 'Вне плана',
      width: 120,
    },
    {
      type: 'date',
      field: 'protocolTime',
      header: 'Осмотр проведён',
      width: 280,
    },
    {
      type: 'text',
      field: 'employee.fullName',
      header: 'Проверяющий',
      width: 255,
    },
    {
      type: 'text',
      field: 'result',
      header: 'Результаты',
    },
    {
      type: 'boolean',
      field: 'defects',
      header: 'Замечания и дефекты',
    },
    {
      type: 'text',
      field: 'conclusion',
      header: 'Заключение',
    },
    {
      type: 'boolean',
      field: 'recommendations',
      header: 'Рекоммендации и комментарии',
    },
  ]);

  protected readonly getRowId = (row: ProtocolDto) => `${row?.id}`;
  protected readonly filter = signal<boolean>(false);
  protected readonly loading = signal<boolean>(false);
  protected readonly protocols = signal<ProtocolDto[]>([]);
  protected selectedProtocol = signal<ProtocolDto | null>(null);
  private readonly table = viewChild<TableComponent>('table');

  private readonly protocolService = inject(ProtocolService);
  private readonly resetService = inject(ResetService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly messageService = inject(MessageService);
  private readonly dialogService = inject(DialogService);

  private resetSubscription?: Subscription;

  ngOnInit() {
    this.resetSubscription = this.resetService.subscribe(async () => {
      await this.loadData();
    });
    this.loadData().then();
  }

  ngOnDestroy(): void {
    this.resetSubscription?.unsubscribe();
  }

  protected async loadData() {
    this.loading.set(true);
    this.protocols.set([]);
    try {
      const protocols = await lastValueFrom(this.protocolService.getProtocols());
      this.protocols.set(protocols);
      this.cdr.detectChanges();
    } finally {
      this.loading.set(false);
    }
  }

  protected async createProtocol() {
    const dialog = this.dialogService.createDialog(ProtocolRegistrationDialogComponent);
    await dialog.init();
    dialog.callbacks = {
      create: async (_, res) => {
        if (res == null) {
          dialog.visible = false;
          return;
        }
        dialog.loading = true;
        try {
          const newProtocol = await lastValueFrom(this.protocolService.createProtocol(res));
          dialog.visible = false;
          this.messageService.add({
            severity: 'success',
            summary: 'Выполнено',
            detail: 'Пользователь добавлен',
          });
          await this.loadData();
          await this.table()?.findAndSelectRow(newProtocol);
        } finally {
          dialog.loading = false;
        }
      },
    };
  }
}
