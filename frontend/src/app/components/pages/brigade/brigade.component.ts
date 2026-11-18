import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { TreeTableComponent } from '../../library/table/tree-table/tree-table.component';
import { Column } from '../../library/table/column';
import { lastValueFrom, Subscription } from 'rxjs';
import { UserDto } from '../../../gen/auth-dto';
import { BrigadeOptimizerService, BrigadeService } from '../../../gen/atom2027-controllers';
import { TreeNode } from '@openng/optimus-ui/api';
import { EventStatusLocaleEnum, ResourceTypeLocaleEnum } from '../events/events.component';
import { ResetService } from '../../../services/reset.service';
import { GlobalLoadingService } from '../../../services/loading.service';
import { Button } from '@openng/optimus-ui/button';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import {
  BrigadeDto,
  EmployeeDto,
  EmployeeQualificationDto,
  EventDto,
} from '../../../gen/atom2027-dto';
import { TableComponent } from '../../library/table/table/table.component';
import { Splitter } from '@openng/optimus-ui/splitter';

@Component({
  imports: [
    TableFastSearchComponent,
    Toolbar,
    TreeTableComponent,
    Button,
    Tooltip,
    TableComponent,
    Splitter,
  ],
  selector: 'pro-legacy-brigade',
  styleUrl: './brigade.component.css',
  templateUrl: './brigade.component.html',
})
export class BrigadeComponent implements OnInit, OnDestroy {
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'ID',
      width: 160,
    },
    {
      type: 'text',
      field: 'name',
      header: 'Номер бригады',
      width: 350,
    },
    {
      type: 'text',
      field: 'brigadeOrderNumber',
      header: 'Порядок исполнения',
      width: 350,
    },
    {
      type: 'enum',
      locale: ResourceTypeLocaleEnum,
      field: 'resourceType',
      decoration: 'tag',
      header: 'Тип ресурса',
      width: 180,
    },
    {
      type: 'text',
      field: 'edifice.name',
      header: 'Наименование объекта',
      width: 350,
    },
    {
      type: 'text',
      field: 'edifice.district',
      header: 'Район объекта',
      width: 350,
    },
    {
      type: 'text',
      field: 'edifice.street',
      header: 'Улица объекта',
      width: 350,
    },
    {
      type: 'text',
      field: 'edifice.houseNumber',
      header: 'Номер дома объекта',
      width: 350,
    },
    {
      type: 'int',
      field: 'edifice.criticalIndex',
      header: 'Индекс критичности объекта',
      width: 120,
    },
  ]);

  protected readonly detailColumns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'Табельный номер',
      width: 240,
    },
    {
      type: 'text',
      field: 'fullName',
      header: 'ФИО',
      width: 350,
    },
    {
      type: 'text',
      field: 'lastName',
      header: 'Фамилия',
      width: 155,
    },
    {
      type: 'text',
      field: 'firstName',
      header: 'Имя',
      width: 155,
    },
    {
      type: 'text',
      field: 'middleName',
      header: 'Отчество',
      width: 155,
    },
    {
      type: 'text',
      field: 'qualifications',
      decoration: 'qualifications',
      header: 'Квалификации',
      width: 350,
    },
    {
      type: 'date',
      field: 'startDateExperience',
      header: 'Дата начала стажа',
      decoration: 'withoutTime',
      width: 235,
    },
  ]);

  protected readonly data = signal<TreeNode[]>([]);

  protected readonly count = signal(0);
  protected readonly loading = signal<boolean>(false);
  protected readonly getRowId = (row: any) =>
    row?.brigadeId != null ? `${row?.brigadeId}/${row?.id}` : `${row?.id}`;

  private readonly brigadeService = inject(BrigadeService);
  private readonly brigadeOptimizerService = inject(BrigadeOptimizerService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly resetService = inject(ResetService);
  private readonly globalLoadingService = inject(GlobalLoadingService);

  private resetSubscription?: Subscription;
  protected selectedBrigadeOrEvent = signal<(BrigadeDto | EventDto) | null>(null);

  protected readonly employees = signal<EmployeeDto[]>([]);
  protected readonly detailGetRowId = (row: EmployeeDto) => `${row?.id}`;
  protected readonly detailFilter = signal<boolean>(false);
  protected readonly detailLoading = signal<boolean>(false);

  ngOnInit() {
    this.resetSubscription = this.resetService.subscribe(async () => {
      this.selectedBrigadeOrEvent.set(null);
      await this.loadDetailData();
      await this.loadData();
    });
    this.loadData().then();
  }

  ngOnDestroy(): void {
    this.resetSubscription?.unsubscribe();
  }

  protected onSelectionChange(event: any) {
    const oldBrigadeId =
      (<any>this.selectedBrigadeOrEvent())?.brigadeId ?? (<any>this.selectedBrigadeOrEvent())?.id;
    this.selectedBrigadeOrEvent.set(event?.data);
    const newBrigadeId =
      (<any>this.selectedBrigadeOrEvent())?.brigadeId ?? (<any>this.selectedBrigadeOrEvent())?.id;
    if (oldBrigadeId !== newBrigadeId) {
      this.loadDetailData().then();
    }
  }

  protected async loadData() {
    this.loading.set(true);
    this.data.set([]);
    this.count.set(0);
    try {
      const brigades = await lastValueFrom(this.brigadeService.getBrigades());
      const nodes: TreeNode[] = [];
      for (let brigade of brigades) {
        const childrenNodes = (brigade.events ?? []).map(e => ({
          key: `${brigade.id}/${e.id}`,
          data: e,
        }));
        nodes.push({
          key: `${brigade.id}`,
          data: brigade,
          children: childrenNodes,
        });
      }
      this.data.set(nodes);
      this.count.set(nodes.length);
      this.cdr.detectChanges();
    } finally {
      this.loading.set(false);
    }
  }

  protected async exportJson() {
    await this.globalLoadingService.withLoader(async () => {
      let json = await lastValueFrom(this.brigadeService.exportBrigadesJson());
      const jsonString = JSON.stringify(json, null, 2);

      const blob = new Blob([jsonString], { type: 'application/json' });
      const url = window.URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = 'бригады.json';
      a.click();

      window.URL.revokeObjectURL(url);
    });
  }

  protected async formBrigades() {
    await this.globalLoadingService.withLoader(async () => {
      await lastValueFrom(this.brigadeOptimizerService.optimizeBrigades());
    });
    await this.loadData();
  }

  protected async loadDetailData() {
    this.detailLoading.set(true);
    this.employees.set([]);
    try {
      const brigadeId =
        (<any>this.selectedBrigadeOrEvent())?.brigadeId ?? (<any>this.selectedBrigadeOrEvent())?.id;
      if (brigadeId == null) {
        this.cdr.detectChanges();
        return;
      }
      const employees = await lastValueFrom(this.brigadeService.getEmployeesForBrigade(brigadeId));
      this.employees.set(employees);
      this.cdr.detectChanges();
      requestAnimationFrame(() => {
        this.cdr.detectChanges();
        window.dispatchEvent(new Event('resize'));
      });
    } finally {
      this.detailLoading.set(false);
    }
  }
}
