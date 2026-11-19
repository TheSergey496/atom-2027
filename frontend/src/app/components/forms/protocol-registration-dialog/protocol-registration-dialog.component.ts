import { Component, inject } from '@angular/core';
import { DialogComponent } from '../../../utils/dialogComponent';
import { EdificeDto, EmployeeDto, ProtocolDto } from '../../../gen/atom2027-dto';
import { Button } from '@openng/optimus-ui/button';
import { Dialog } from '@openng/optimus-ui/dialog';
import { Textarea } from '@openng/optimus-ui/textarea';
import { FormsModule } from '@angular/forms';
import { FloatLabel } from '@openng/optimus-ui/floatlabel';
import { FloatLabelTransitionFixDirective } from '../../../directives/float-label-transition-fix.directive';
import { InputText } from '@openng/optimus-ui/inputtext';
import { SelectButton } from '@openng/optimus-ui/selectbutton';
import { DatePicker } from '@openng/optimus-ui/datepicker';
import { AutoComplete, AutoCompleteCompleteEvent } from '@openng/optimus-ui/autocomplete';
import { EdificeService, EmployeeService } from '../../../gen/atom2027-controllers';
import { lastValueFrom } from 'rxjs';

@Component({
  imports: [
    Button,
    Dialog,
    Textarea,
    FormsModule,
    FloatLabel,
    FloatLabelTransitionFixDirective,
    InputText,
    SelectButton,
    DatePicker,
    AutoComplete,
  ],
  selector: 'pro-legacy-protocol-registration-dialog',
  styleUrl: './protocol-registration-dialog.component.css',
  templateUrl: './protocol-registration-dialog.component.html',
})
export class ProtocolRegistrationDialogComponent extends DialogComponent<ProtocolDto, 'create'> {
  protected stateOptions = [
    { label: 'Плановый/повторный', value: Type.NORMAL },
    { label: 'Вне плана', value: Type.NON_PLAN },
  ];

  protected selectedType = Type.NORMAL;

  protected allEmployees: EmployeeDto[] = [];
  protected employees: EmployeeDto[] = [];

  protected allEdifices: EdificeDto[] = [];
  protected edifices: EdificeDto[] = [];

  private readonly employeeService = inject(EmployeeService);
  private readonly edificeService = inject(EdificeService);

  public constructor() {
    super();
    this.res = new ProtocolDto();
    this.loading = true;
  }

  public async init(protocolId?: number) {
    if (protocolId == null) {
      this.res = new ProtocolDto();
    }
    try {
      this.allEmployees = await lastValueFrom(this.employeeService.getEmployees());
      this.employees = [...this.allEmployees];

      this.allEdifices = await lastValueFrom(this.edificeService.getEdifices());
      this.edifices = [...this.allEdifices];
    } finally {
      this.loading = false;
    }
  }

  searchEmployee(event: AutoCompleteCompleteEvent) {
    this.employees = this.allEmployees.filter(e =>
      e.fullName?.toLowerCase().includes(event.query.toLowerCase()),
    );
  }

  searchEdifices(event: AutoCompleteCompleteEvent) {
    this.edifices = this.allEdifices.filter(e =>
      e.name?.toLowerCase().includes(event.query.toLowerCase()),
    );
  }

  protected ok() {
    void this._callbacks.create(this, this.res);
  }
  protected close() {
    this.visible = false;
  }

  protected onSelectedTypeChange(type: Type) {
    this.selectedType = type;
    if (this.res != null) {
      this.res.edifice = undefined;
      this.res.check = undefined;
    }
  }

  protected readonly Type = Type;
}

enum Type {
  NORMAL,
  NON_PLAN,
}
