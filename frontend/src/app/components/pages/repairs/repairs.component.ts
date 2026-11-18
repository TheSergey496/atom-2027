import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit, signal, } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { TableComponent } from '../../library/table/table/table.component';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { Column } from '../../library/table/column';
import { BrigadeEventDto } from '../../../gen/atom2027-dto';
import { UserDto } from '../../../gen/auth-dto';
import { BrigadeService } from '../../../gen/atom2027-controllers';
import { ResetService } from '../../../services/reset.service';
import { lastValueFrom, Subscription } from 'rxjs';
import { ResourceTypeLocaleEnum } from '../events/events.component';

@Component({
  imports: [Button, TableComponent, TableFastSearchComponent, Toolbar],
  selector: 'pro-legacy-repairs',
  styleUrl: './repairs.component.css',
  templateUrl: './repairs.component.html',
})
export class RepairsComponent implements OnInit, OnDestroy {
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'event.id',
      header: 'ID события',
      width: 120,
    },
    {
      type: 'enum',
      locale: ResourceTypeLocaleEnum,
      field: 'event.resourceType',
      decoration: 'tag',
      header: 'Тип ресурса',
      width: 350,
    },
    {
      type: 'date',
      field: 'event.occurredAt',
      header: 'Время возникновения аварии',
      width: 350,
    },
    {
      type: 'date',
      field: 'startDate',
      header: 'Время начала ремонтных работ',
      width: 350,
    },
    {
      type: 'date',
      field: 'expectedEndTime',
      header: 'Планируемое время завершения ликвидации аварии',
      width: 350,
    },
    {
      type: 'text',
      field: 'averageResolutionTimeStr',
      header: 'Базовое время ликвидации аварии',
      width: 350,
    },
    {
      type: 'text',
      field: 'actualResolutionTimeStr',
      header: 'Фактическое время ликвидации аварии',
      width: 350,
    },
    {
      type: 'text',
      field: 'brigade.name',
      header: 'Номер ответственной бригады',
      width: 350,
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

  protected readonly events = signal<BrigadeEventDto[]>([]);

  protected readonly getRowId = (row: UserDto) => `${row?.id}`;
  protected readonly filter = signal<boolean>(false);
  protected readonly loading = signal<boolean>(false);

  private readonly brigadeService = inject(BrigadeService);
  private readonly resetService = inject(ResetService);
  private readonly cdr = inject(ChangeDetectorRef);

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
    this.events.set([]);
    try {
      const events = await lastValueFrom(this.brigadeService.getBrigadeEvents());
      this.events.set(events);
      this.cdr.detectChanges();
    } finally {
      this.loading.set(false);
    }
  }
}
