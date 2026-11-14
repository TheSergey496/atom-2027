import { Component, computed, inject, input, output, Renderer2, signal, viewChild, viewChildren } from '@angular/core';
import { Column, TableColumn, ColumnUtils } from '../column';
import {
  FilterMatchMode,
  FilterMetadata,
  FilterService,
  MenuItem,
  MessageService,
  SelectItem,
  SortMeta,
} from '@openng/optimus-ui/api';
import { ExcelExportService } from '../../../../services/excel-export.service';
import { LargeExcelExportService } from '../../../../services/large-excel-export.service';
import { ColumnFilter, FrozenColumn, ReorderableColumn, SelectableRow, SortableColumn, SortIcon, Table } from '@openng/optimus-ui/table';
import { CustomFilterMatchMode } from '../custom-filter-match-mode';

@Component({
  imports: [],
  selector: 'pro-legacy-tree-table',
  styleUrl: './tree-table.component.css',
  templateUrl: './tree-table.component.html',
})
export class TreeTableComponent {
  public readonly columns = input<Column[]>([]);
  public readonly value = input<any[]>([]);
  public readonly selectionMode = input<'single' | 'multiple'>('single');
  public readonly filter = input<boolean>(false);
  public readonly headerRowHeight = input<number>(50);
  public readonly filterRowHeight = input<number>(30);
  public readonly rowHeight = input<number>(30);
  public readonly loading = input<boolean>(false);
  public readonly defaultSort = input<SortMeta[] | undefined>(undefined);
  public readonly getRowId = input<((row: any) => string) | undefined>(undefined);
  public readonly checkboxSelection = input<boolean>(false);

  public readonly selection = input<(any | undefined) | any[]>(undefined);
  public readonly selectionChange = output<(any | undefined) | any[]>();

  protected columnsByField: { [key: string]: TableColumn } = {};
  protected tableValue: any[] = [];
  protected tableDefaultSort: SortMeta[] | undefined = undefined;
  protected tableSelection: (any | undefined) | any[] = undefined;
  protected readonly tableDataKey = computed(() => {
    return this.getRowId() != null ? this.rowIdField : undefined;
  });
  protected readonly tableCheckboxSelection = computed(() => {
    return this.selectionMode() === 'multiple' && this.checkboxSelection();
  });

  protected tableColumns: TableColumn[] = [];
  protected tableGlobalFilterFields: string[] = [];
  protected get tableFrozenLeftColumns() {
    return this.tableColumns.filter(c => c.frozen === 'left');
  }
  protected get tableFrozenRightColumns() {
    return this.tableColumns.filter(c => c.frozen === 'right');
  }

  protected readonly blockingMask = signal<boolean>(false);
  protected readonly tableHasFilter = signal<boolean>(false);
  protected tableAppliedFilters: { column: TableColumn; label: string; valueLocale: string }[] = [];

  protected readonly tableRowSelectable = (row: { data: any; index: number }) =>
    row?.data != null && row.data[this.deletedMessageField] == null;

  protected readonly FilterMatchMode = FilterMatchMode;
  protected readonly CustomFilterMatchMode = CustomFilterMatchMode;
  protected readonly ColumnUtils = ColumnUtils;
  protected readonly rowIdField = '__rowId';
  protected readonly deletedMessageField = '__deletedMessage';
  protected readonly indicatorColumnField = '__indicator';

  protected readonly indicatorMenuOptions: MenuItem[] = [
    // {
    //   label: 'Сбросить все сортировки',
    //   icon: 'pi pi-sort-alt-slash',
    //   command: () => this.resetAllSorts(),
    // },
    // {
    //   label: 'Очистить все фильтры',
    //   icon: 'pi pi-filter-slash',
    //   command: () => this.resetAllFilters(),
    // },
    // {
    //   label: 'Сбросить все настройки',
    //   icon: 'pi pi-refresh',
    //   command: () => this.resetTable(),
    // },
  ];

  protected columnMenuOptions: { [key: string]: MenuItem[] } = {};
  private readonly freezeLeftMenuOptionId = 'freezeLeft';
  private readonly freezeRightMenuOptionId = 'freezeLeft';
  private readonly unfreezeMenuOptionId = 'freezeLeft';

  private readonly renderer = inject(Renderer2);
  private readonly filterService = inject(FilterService);
  private readonly excelExportService = inject(ExcelExportService);
  private readonly largeExcelExportService = inject(LargeExcelExportService);
  private readonly messageService = inject(MessageService);
  private readonly table = viewChild.required(Table);
  private readonly rows = viewChildren(SelectableRow);
  private readonly reorderableColumns = viewChildren(ReorderableColumn);
  private readonly frozenColumns = viewChildren(FrozenColumn);
  private readonly columnFilters = viewChildren(ColumnFilter);
  private readonly sortableColumns = viewChildren(SortableColumn);
  private readonly sortableColumnIcons = viewChildren(SortIcon);

  private readonly indicatorColumn: Column = {
    field: this.indicatorColumnField,
    header: '',
    type: 'text',
    width: 32,
    frozen: 'left',
    getter: () => undefined,
  };

  private headerBackground?: HTMLDivElement;
  private headerBackgroundListeners: any[] = [];

  private readonly numberMatchModeOptions: SelectItem<string>[] = [
    {
      label: 'Равно',
      value: FilterMatchMode.EQUALS,
    },
    {
      label: 'Не равно',
      value: FilterMatchMode.NOT_EQUALS,
    },
    {
      label: 'Больше',
      value: FilterMatchMode.GREATER_THAN,
    },
    {
      label: 'Больше или равно',
      value: FilterMatchMode.GREATER_THAN_OR_EQUAL_TO,
    },
    {
      label: 'Меньше',
      value: FilterMatchMode.LESS_THAN,
    },
    {
      label: 'Меньше или равно',
      value: FilterMatchMode.LESS_THAN_OR_EQUAL_TO,
    },
  ];

  private readonly textMatchModeOptions: SelectItem<string>[] = [
    {
      label: 'Содержит',
      value: FilterMatchMode.CONTAINS,
    },
    {
      label: 'Не содержит',
      value: FilterMatchMode.NOT_CONTAINS,
    },
    {
      label: 'Начинается с',
      value: FilterMatchMode.STARTS_WITH,
    },
    {
      label: 'Заканчивается на',
      value: FilterMatchMode.ENDS_WITH,
    },
    {
      label: 'Равно',
      value: FilterMatchMode.EQUALS,
    },
    {
      label: 'Не равно',
      value: FilterMatchMode.NOT_EQUALS,
    },
  ];

  private readonly dateMatchModeOptions: SelectItem<string>[] = [
    {
      label: 'Равно',
      value: FilterMatchMode.DATE_IS,
    },
    {
      label: 'Не равно',
      value: FilterMatchMode.DATE_IS_NOT,
    },
    {
      label: 'До',
      value: FilterMatchMode.DATE_BEFORE,
    },
    {
      label: 'После',
      value: FilterMatchMode.DATE_AFTER,
    },
    {
      label: 'Между',
      value: FilterMatchMode.BETWEEN,
    },
  ];

  private readonly booleanMatchModeOptions: SelectItem<string>[] = [
    {
      label: 'Равно',
      value: CustomFilterMatchMode.NULLABLE_EQUALS,
    },
    {
      label: 'Не равно',
      value: CustomFilterMatchMode.NULLABLE_NOT_EQUALS,
    },
  ];

  private readonly enumMatchModeOptions: SelectItem<string>[] = [
    {
      label: 'Равно',
      value: CustomFilterMatchMode.NULLABLE_EQUALS,
    },
    {
      label: 'Не равно',
      value: CustomFilterMatchMode.NULLABLE_NOT_EQUALS,
    },
  ];

  protected readonly numberFormatters: { [key: string]: Intl.NumberFormat } = {};

  protected readonly booleanEqualsOptions: SelectItem<boolean>[] = [
    {
      label: 'Да',
      value: true,
    },
    {
      label: 'Нет',
      value: false,
    },
  ];

  protected readonly booleanEqualsOptionsWithEmpty: SelectItem<
    boolean | typeof CustomFilterMatchMode.nullReservedFilterWord
  >[] = [
    ...this.booleanEqualsOptions,
    {
      label: 'Не указано',
      value: CustomFilterMatchMode.nullReservedFilterWord,
    },
  ];
}
