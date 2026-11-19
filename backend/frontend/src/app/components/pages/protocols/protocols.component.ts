import { Component } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { TableComponent } from '../../library/table/table/table.component';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { Tooltip } from '@openng/optimus-ui/tooltip';

@Component({
  imports: [Button, TableComponent, TableFastSearchComponent, Toolbar, Tooltip],
  selector: 'pro-legacy-protocols',
  styleUrl: './protocols.component.css',
  templateUrl: './protocols.component.html',
})
export class ProtocolsComponent {}
