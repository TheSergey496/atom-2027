import { Component, inject, signal } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { TableComponent } from '../../library/table/table/table.component';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { Column } from '../../library/table/column';
import { UserDto } from '../../../gen/auth-dto';
import { lastValueFrom } from 'rxjs';
import { EdificeDto } from '../../../gen/atom2027-dto';
import { EdificeService } from '../../../gen/atom2027-controllers';
import { EventStatusLocaleEnum } from '../events/events.component';

@Component({
  imports: [Button, TableComponent, TableFastSearchComponent, Toolbar],
  selector: 'pro-legacy-edifices',
  styleUrl: './edifices.component.css',
  templateUrl: './edifices.component.html',
})
export class EdificesComponent {
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'ID',
      width: 120,
    },
    {
      type: 'text',
      field: 'name',
      header: 'Наименование',
      width: 350,
    },
    {
      type: 'text',
      field: 'district',
      header: 'Район',
      width: 350,
    },
    {
      type: 'text',
      field: 'street',
      header: 'Улица',
      width: 350,
    },
    {
      type: 'text',
      field: 'houseNumber',
      header: 'Номер дома',
      width: 350,
    },
    {
      type: 'enum',
      field: 'waterActiveEvent.eventStatus.name',
      locale: EventStatusLocaleEnum,
      getter: (row: EdificeDto) => {
        return row?.waterActiveEvent?.eventStatus?.name ?? 'Норма';
      },
      header: 'Статус воды',
      decoration: 'tag',
      width: 350,
    },
    {
      type: 'text',
      field: 'energyActiveEvent.eventStatus.name',
      getter: (row: EdificeDto) => {
        return row?.energyActiveEvent?.eventStatus?.name ?? 'Норма';
      },
      header: 'Статус электроэнергии',
      decoration: 'tag',
      width: 350,
    },
    {
      type: 'int',
      field: 'criticalIndex',
      header: 'Индекс критичности',
      width: 120,
    },
  ]);

  protected readonly edificies = signal<EdificeDto[]>([]);

  protected readonly getRowId = (row: UserDto) => `${row?.id}`;
  protected readonly filter = signal<boolean>(false);
  protected readonly loading = signal<boolean>(false);

  private readonly edificeService = inject(EdificeService);

  ngOnInit() {
    this.loadData().then();
  }

  protected async loadData() {
    this.loading.set(true);
    this.edificies.set([]);
    try {
      const edifices = await lastValueFrom(this.edificeService.getEdifices());
      this.edificies.set(edifices);
    } finally {
      this.loading.set(false);
    }
  }
}
