import { Component, inject, OnDestroy, signal } from '@angular/core';
import { DialogComponent } from '../../../utils/dialogComponent';
import { CheckDto, EdificeDto, EmployeeDto } from '../../../gen/atom2027-dto';
import { CheckService, EdificeService, EmployeeService } from '../../../gen/atom2027-controllers';
import { lastValueFrom } from 'rxjs';
import { ButtonModule } from '@openng/optimus-ui/button';
import { DialogModule } from '@openng/optimus-ui/dialog';
import { MessageService } from '@openng/optimus-ui/api';
import { MessageServiceKey } from '../../../app';
import { CheckTypeRecord } from '../../../gen/entities-enums';
import { CheckTypeLocaleEnum } from '../../../enums/CheckTypeLocaleEnum';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FloatLabelModule } from '@openng/optimus-ui/floatlabel';
import { FloatLabelTransitionFixDirective } from '../../../directives/float-label-transition-fix.directive';
import { SelectModule } from '@openng/optimus-ui/select';
import { DatePickerModule } from '@openng/optimus-ui/datepicker';
import { TextareaModule } from '@openng/optimus-ui/textarea';
import { InputMaskModule } from '@openng/optimus-ui/inputmask';
import { AutoComplete, AutoCompleteCompleteEvent } from '@openng/optimus-ui/autocomplete';
import { FormMessageComponent } from '../../library/form-message/form-message.component';

@Component({
  imports: [
    ButtonModule,
    DialogModule,
    FormsModule,
    ReactiveFormsModule,
    FloatLabelModule,
    FloatLabelTransitionFixDirective,
    SelectModule,
    DatePickerModule,
    TextareaModule,
    InputMaskModule,
    AutoComplete,
    FormMessageComponent,
  ],
  selector: 'pro-legacy-check-edit-dialog',
  styleUrl: './check-edit-dialog.component.css',
  templateUrl: './check-edit-dialog.component.html',
})
export class CheckEditDialogComponent
  extends DialogComponent<CheckDto, 'save'>
  implements OnDestroy
{
  protected check: CheckDto = new CheckDto();

  protected readonly header = signal<string | undefined>('Редактирование проверки');
  protected edifices: EdificeDto[] = [];
  protected readonly edificeSuggestions = signal<EdificeDto[]>([]);
  protected employees: EmployeeDto[] = [];
  protected readonly employeeSuggestions = signal<EmployeeDto[]>([]);
  protected readonly checkTypes = signal<{ label: string; value: string }[]>([]);

  private readonly checkService = inject(CheckService);
  private readonly edificeService = inject(EdificeService);
  private readonly employeeService = inject(EmployeeService);
  private readonly messageService = inject(MessageService);

  protected readOnlyMode = signal(false);

  private readonly intervalId: any;
  protected dateValid = signal(true);

  constructor() {
    super();
    this.intervalId = setInterval(() => {
      try {
        if (this.check?.planDate != null) {
          this.dateValid.set(this.check?.planDate.getTime() > new Date().getTime());
        } else {
          this.dateValid.set(true);
        }
      } catch (e) {}
    }, 1000);
  }

  override ngOnDestroy() {
    clearInterval(this.intervalId);
  }

  public async init(checkId?: number, readOnlyMode = false) {
    this.loading = true;
    this.readOnlyMode.set(readOnlyMode);
    try {
      if (checkId != null) {
        this.header.set(this.readOnlyMode() ? 'Просмотр проверки' : 'Редактирование проверки');
        const check = await lastValueFrom(this.checkService.getCheck(checkId));
        if (check == null) {
          this.messageService.add({
            key: MessageServiceKey.OK,
            sticky: true,
            severity: 'error',
            summary: 'Ошибка',
            detail: `Не найдена проверка с id = ${checkId}`,
          });
          this.visible = false;
          return;
        }
        if (check.planDate != null) {
          check.planDate = new Date(check.planDate);
        }

        try {
          if (this.check?.planDate != null) {
            this.dateValid.set(this.check?.planDate.getTime() > new Date().getTime());
          } else {
            this.dateValid.set(true);
          }
        } catch (e) {}

        this.check = check;
      } else {
        this.header.set('Назначение проверки');
      }

      this.employees = await lastValueFrom(this.employeeService.getEmployees());
      this.employeeSuggestions.set([...this.employees]);
      this.edifices = await lastValueFrom(this.edificeService.getEdifices());
      this.edificeSuggestions.set([...this.edifices]);
      this.checkTypes.set(
        (this.readOnlyMode()
          ? [CheckTypeRecord.NON_PLAN, CheckTypeRecord.PLAN, CheckTypeRecord.REPEATED]
          : [CheckTypeRecord.PLAN, CheckTypeRecord.REPEATED]
        ).map(ct => ({
          label: CheckTypeLocaleEnum[ct],
          value: ct,
        })),
      );
    } catch (e) {
      this.visible = false;
      throw e;
    } finally {
      this.loading = false;
    }
  }

  protected async save() {
    await this._callbacks.save(this, this.check);
  }

  protected edificeCompleteMethod(event: AutoCompleteCompleteEvent) {
    this.edificeSuggestions.set(
      this.edifices.filter(e => e.name?.toLowerCase()?.includes(event.query?.toLowerCase())),
    );
  }

  protected employeeCompleteMethod(event: AutoCompleteCompleteEvent) {
    this.employeeSuggestions.set(
      this.employees.filter(e => e.fullName?.toLowerCase()?.includes(event.query?.toLowerCase())),
    );
  }

  protected onPlanDateChange(newDate: Date) {
    this.check.planDate = newDate;
    try {
      if (this.check?.planDate != null) {
        this.dateValid.set(this.check?.planDate.getTime() > new Date().getTime());
      } else {
        this.dateValid.set(true);
      }
    } catch (e) {}
  }
}
