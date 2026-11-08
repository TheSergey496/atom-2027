import { Component, input } from '@angular/core';
import { TableComponent } from '../table.component';
import { InputTextModule } from '@openng/optimus-ui/inputtext';
import { IconFieldModule } from '@openng/optimus-ui/iconfield';
import { InputIconModule } from '@openng/optimus-ui/inputicon';

@Component({
  imports: [InputTextModule, IconFieldModule, InputIconModule],
  selector: 'pro-legacy-table-fast-search',
  styleUrl: './table-fast-search.component.css',
  templateUrl: './table-fast-search.component.html',
})
export class TableFastSearchComponent {
  public readonly table = input.required<TableComponent>();

  protected applyGlobalFilter(value: string) {
    this.table()?.applyGlobalFilter(value);
  }
}
