import { ChangeDetectorRef, Component, inject, signal } from '@angular/core';
import { DialogComponent } from '../../../utils/dialogComponent';
import { CheckDto, EdificeDto, EmployeeDto, ProtocolDto } from '../../../gen/atom2027-dto';
import { Button } from '@openng/optimus-ui/button';
import { Dialog } from '@openng/optimus-ui/dialog';
import { Textarea } from '@openng/optimus-ui/textarea';
import { FormsModule } from '@angular/forms';
import { FloatLabel } from '@openng/optimus-ui/floatlabel';
import { FloatLabelTransitionFixDirective } from '../../../directives/float-label-transition-fix.directive';
import { InputText } from '@openng/optimus-ui/inputtext';
import { SelectButton } from '@openng/optimus-ui/selectbutton';
import { AutoComplete, AutoCompleteCompleteEvent } from '@openng/optimus-ui/autocomplete';
import {
  CheckService,
  EdificeService,
  EmployeeService,
  ProtocolService,
} from '../../../gen/atom2027-controllers';
import { lastValueFrom } from 'rxjs';
import { CheckTypeRecord } from '../../../gen/entities-enums';
import { FormMessageComponent } from '../../library/form-message/form-message.component';

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
    AutoComplete,
    FormMessageComponent,
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

  protected allChecks: CheckDto[] = [];
  protected checks: CheckDto[] = [];

  protected allEmployees: EmployeeDto[] = [];
  protected employees: EmployeeDto[] = [];

  protected allEdifices: EdificeDto[] = [];
  protected edifices: EdificeDto[] = [];

  protected readonly header = signal<string | undefined>('Создание Протокола');

  private readonly protocolService = inject(ProtocolService);
  private readonly checkService = inject(CheckService);
  private readonly employeeService = inject(EmployeeService);
  private readonly edificeService = inject(EdificeService);
  private readonly cdr = inject(ChangeDetectorRef);

  protected readOnlyMode = signal(false);

  protected employeeError = signal<string | null>(null);

  public constructor() {
    super();
    this.res = new ProtocolDto();
    this.loading = true;
  }

  public async init(protocolId?: number, readOnlyMode = false) {
    this.readOnlyMode.set(readOnlyMode);
    try {
      if (protocolId == null) {
        this.header.set('Создание протокола');
        this.res = new ProtocolDto();
      } else {
        this.header.set(this.readOnlyMode() ? 'Просмотр протокола' : 'Редактирование проверки');
        this.res = await lastValueFrom(this.protocolService.getProtocol(protocolId));
        this.selectedType =
          this.res.check?.checkType?.toString() === CheckTypeRecord.NON_PLAN.toString()
            ? Type.NON_PLAN
            : Type.NORMAL;
      }
      this.cdr.detectChanges();

      this.allChecks = await lastValueFrom(this.checkService.getChecks(false, true));
      this.checks = [...this.allChecks];

      this.allEmployees = (await lastValueFrom(this.employeeService.getEmployees()));
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

  protected searchChecks(event: AutoCompleteCompleteEvent) {
    this.checks = this.allChecks.filter(e =>
      e.checkId?.toString().includes(event.query.toString()),
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

  protected onEmployeeChange(newValue: EmployeeDto) {
    this.res!.employee = newValue;
    this.calculateEmployeeError();
  }

  protected onCheckChange(newValue: CheckDto) {
    this.res!.check = newValue;
    this.calculateEmployeeError();
  }

  protected onEdificeChange(newValue: EdificeDto) {
    this.res!.edifice = newValue;
    this.calculateEmployeeError();
  }

  private calculateEmployeeError() {
    const edifice = this.res?.check?.edifice ?? this.res?.edifice;
    if (edifice != null && this.res!.employee != null) {
      const badForWater = edifice.hasWater && !this.res!.employee.qualifications?.includes('Вода');
      const badForEnergy =
        edifice.hasEnergy && !this.res!.employee.qualifications?.includes('Электроэнергия');
      if (badForEnergy && badForWater) {
        this.employeeError.set(
          'Выбранный проверяющий должен обладать квалификациями по воде и электроэнергии',
        );
      } else if (badForWater) {
        this.employeeError.set('Выбранный проверяющий должен обладать квалификацией по воде');
      } else if (badForEnergy) {
        this.employeeError.set(
          'Выбранный проверяющий должен обладать квалификацией по электроэнергии',
        );
      } else {
        this.employeeError.set(null);
      }
    } else {
      this.employeeError.set(null);
    }
  }
}

enum Type {
  NORMAL,
  NON_PLAN,
}
