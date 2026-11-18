import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { TableComponent } from '../../library/table/table/table.component';
import { TableFastSearchComponent } from '../../library/table/table-fast-search/table-fast-search.component';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { Column } from '../../library/table/column';
import { EmployeeDto, EmployeeQualificationDto } from '../../../gen/atom2027-dto';
import { EmployeeService } from '../../../gen/atom2027-controllers';
import { lastValueFrom, Subscription } from 'rxjs';
import { Splitter } from '@openng/optimus-ui/splitter';
import { ResourceTypeLocaleEnum } from '../events/events.component';
import { ResetService } from '../../../services/reset.service';

@Component({
  imports: [Button, TableComponent, TableFastSearchComponent, Toolbar, Splitter],
  selector: 'pro-legacy-employee',
  styleUrl: './employee.component.css',
  templateUrl: './employee.component.html',
})
export class EmployeeComponent implements OnInit, OnDestroy {
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'Табельный номер',
      width: 240,
    },
    {
      type: 'text',
      field: 'fullName',
      header: 'ФИО',
      width: 350,
    },
    {
      type: 'text',
      field: 'lastName',
      header: 'Фамилия',
      width: 350,
    },
    {
      type: 'text',
      field: 'firstName',
      header: 'Имя',
      width: 350,
    },
    {
      type: 'text',
      field: 'middleName',
      header: 'Отчество',
      width: 350,
    },
    {
      type: 'text',
      field: 'qualifications',
      header: 'Квалификации',
      width: 350,
    },
    {
      type: 'date',
      field: 'startDateExperience',
      header: 'Дата начала стажа',
      decoration: 'withoutTime',
      width: 350,
    },
  ]);

  protected readonly detailColumns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'ID',
      width: 120,
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
      type: 'number',
      field: 'proff',
      header: 'Уровень профессионализма',
      width: 350,
    },
  ]);

  protected selectedEmployee = signal<EmployeeDto | null>(null);
  protected readonly employees = signal<EmployeeDto[]>([]);
  private readonly cdr = inject(ChangeDetectorRef);

  protected readonly getRowId = (row: EmployeeDto) => `${row?.id}`;
  protected readonly filter = signal<boolean>(false);
  protected readonly loading = signal<boolean>(false);

  protected readonly employeeQualifications = signal<EmployeeQualificationDto[]>([]);
  protected readonly detailGetRowId = (row: EmployeeQualificationDto) => `${row?.id}`;
  protected readonly detailFilter = signal<boolean>(false);
  protected readonly detailLoading = signal<boolean>(false);

  private readonly employeeService = inject(EmployeeService);
  private readonly resetService = inject(ResetService);

  private resetSubscription?: Subscription;

  ngOnInit() {
    this.loadData().then();

    this.resetSubscription = this.resetService.subscribe(async () => {
      this.selectedEmployee.set(null);
      await this.loadDetailData();
      await this.loadData();
    });
    this.loadData().then();
  }

  ngOnDestroy(): void {
    this.resetSubscription?.unsubscribe();
  }

  protected async loadData() {
    this.loading.set(true);
    this.employees.set([]);
    try {
      const edifices = await lastValueFrom(this.employeeService.getEmployees());
      this.employees.set(edifices);
      this.cdr.detectChanges();
    } finally {
      this.loading.set(false);
    }
  }

  protected async loadDetailData() {
    this.detailLoading.set(true);
    this.employeeQualifications.set([]);
    try {
      if (this.selectedEmployee()?.id == null) {
        this.cdr.detectChanges();
        return;
      }
      const edifices = await lastValueFrom(
        this.employeeService.getQualifications(this.selectedEmployee()!.id!),
      );
      this.employeeQualifications.set(edifices);
      this.cdr.detectChanges();
    } finally {
      this.detailLoading.set(false);
    }
  }
}
