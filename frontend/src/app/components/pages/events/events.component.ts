import { Component, effect, inject, input, signal } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { TableComponent } from '../../library/table/table/table.component';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { Column } from '../../library/table/column';
import { EdificeDto, EventDto } from '../../../gen/atom2027-dto';
import { UserDto } from '../../../gen/auth-dto';
import { EventService } from '../../../gen/atom2027-controllers';

@Component({
  imports: [Button, TableComponent, TableFastSearchComponent, Toolbar],
  selector: 'pro-legacy-events',
  styleUrl: './events.component.css',
  templateUrl: './events.component.html',
})
export class EventsComponent {
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'ID',
      width: 120,
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
    {
      type: 'text',
      field: 'eventStatusName',
      header: 'Тип события',
      decoration: 'tag',
      width: 350,
    },
  ]);

  protected readonly events = signal<EventDto[]>([]);

  protected readonly getRowId = (row: UserDto) => `${row?.id}`;
  protected readonly filter = signal<boolean>(false);
  protected readonly loading = signal<boolean>(false);

  private readonly eventService = inject(EventService);

  public forEdifice = input(true);
  public edifice = input<EdificeDto | null>(null);

  constructor() {
    effect(() => {
      const _ = this.edifice();
      const __ = this.forEdifice();
      this.loadData().then();
    });
  }

  protected async loadData() {
    // this.loading.set(true);
    // this.events.set([]);
    // try {
    //   if (this.forEdifice() && this.edifice() == null) {
    //     return;
    //   }
    //   const edifices = await lastValueFrom(this.events.getEdifices());
    //   this.edificies.set(edifices);
    // } finally {
    //   this.loading.set(false);
    // }
  }
}
