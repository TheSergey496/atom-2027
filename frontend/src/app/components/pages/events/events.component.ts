import { AfterViewInit, ChangeDetectorRef, Component, effect, inject, input, OnDestroy, signal } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { TableComponent } from '../../library/table/table/table.component';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { Column } from '../../library/table/column';
import { EdificeDto, EventDto } from '../../../gen/atom2027-dto';
import { UserDto } from '../../../gen/auth-dto';
import { EventService } from '../../../gen/atom2027-controllers';
import { lastValueFrom, Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import {SelectButtonModule} from "@openng/optimus-ui/selectbutton";
import { ResetService } from '../../../services/reset.service';

@Component({
  imports: [
    Button,
    TableComponent,
    TableFastSearchComponent,
    Toolbar,
    SelectButtonModule,
    FormsModule,
  ],
  selector: 'pro-legacy-events',
  styleUrl: './events.component.css',
  templateUrl: './events.component.html',
})
export class EventsComponent implements AfterViewInit, OnDestroy {
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'ID',
      width: 120,
    },
    {
      type: 'enum',
      locale: EventStatusLocaleEnum,
      field: 'eventStatus.name',
      header: 'Тип события',
      decoration: 'tag',
      width: 350,
    },
    {
      type: 'date',
      field: 'occurredAt',
      header: 'Дата',
      width: 350,
    },
    {
      type: 'enum',
      locale: ResourceTypeLocaleEnum,
      field: 'resourceType',
      decoration: 'tag',
      header: 'Тип ресурса',
      width: 350,
    },
    {
      type: 'boolean',
      field: 'isArchived',
      header: 'Архив',
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

  protected readonly events = signal<EventDto[]>([]);

  protected readonly getRowId = (row: UserDto) => `${row?.id}`;
  protected readonly filter = signal<boolean>(false);
  protected readonly loading = signal<boolean>(false);

  private readonly eventService = inject(EventService);
  private readonly resetService = inject(ResetService);
  private readonly cdr = inject(ChangeDetectorRef);

  public forEdifice = input(false);
  public edifice = input<EdificeDto | null>(null);

  private resetSubscription?: Subscription;

  protected showArchived = false;
  protected showArchivedOptions = [
    {
      label: 'Активные',
      value: false,
    },
    {
      label: 'Архив',
      value: true,
    },
  ];

  constructor() {
    effect(() => {
      const _ = this.edifice();
      const __ = this.forEdifice();
      this.loadData().then();
    });
  }

  ngAfterViewInit() {
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
      if (this.forEdifice() && this.edifice() == null) {
        return;
      }
      const events = await lastValueFrom(
        this.eventService.getEvents(this.edifice()?.id, this.showArchived),
      );
      this.events.set(events);
      this.cdr.detectChanges();
    } finally {
      this.loading.set(false);
    }
  }

  protected onShowArchivedChange(newValue: boolean) {
    this.showArchived = newValue;
    this.loadData().then();
  }
}

export enum ResourceTypeLocaleEnum {
  water = 'Вода',
  energy = 'Электроэнергия'
}

export const EventStatusLocaleEnum = {
  'Активная авария': 'Активная авария',
  'Ремонтные работы': 'Ремонтные работы',
  'Норма': 'Норма'
}
