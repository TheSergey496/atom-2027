import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { TreeTableComponent } from '../../library/table/tree-table/tree-table.component';
import { Column } from '../../library/table/column';
import { lastValueFrom, Subscription } from 'rxjs';
import { UserDto } from '../../../gen/auth-dto';
import { BrigadeService } from '../../../gen/atom2027-controllers';
import { TreeNode } from '@openng/optimus-ui/api';
import { EventStatusLocaleEnum, ResourceTypeLocaleEnum } from '../events/events.component';
import { ResetService } from '../../../services/reset.service';
import { GlobalLoadingService } from '../../../services/loading.service';
import { Button } from '@openng/optimus-ui/button';
import { Tooltip } from '@openng/optimus-ui/tooltip';

@Component({
  imports: [TableFastSearchComponent, Toolbar, TreeTableComponent, Button, Tooltip],
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
      header: 'Наименование',
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

  protected readonly data = signal<TreeNode[]>([]);

  protected readonly count = signal(0);
  protected readonly loading = signal<boolean>(false);
  protected readonly getRowId = (row: any) =>
    row?.brigadeId != null ? `${row?.brigadeId}/${row?.id}` : `${row?.id}`;

  private readonly brigadeService = inject(BrigadeService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly resetService = inject(ResetService);
  private readonly globalLoadingService = inject(GlobalLoadingService);

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
}
