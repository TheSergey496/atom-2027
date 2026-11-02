import {
  afterRenderEffect,
  ChangeDetectorRef,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  Renderer2,
  untracked,
  viewChild,
  viewChildren,
} from '@angular/core';
import {
  ColumnFilter,
  ReorderableColumn,
  SelectableRow,
  SortableColumn,
  SortIcon,
  Table,
  TableModule,
} from '@openng/optimus-ui/table';
import { ObjectUtils } from '@openng/optimus-ui/utils';
import { DomHandler } from '@openng/optimus-ui/dom';
import {
  FilterMatchMode,
  FilterMetadata,
  FilterService,
  MenuItem,
  SelectItem,
  SortMeta,
} from '@openng/optimus-ui/api';
import { MenuModule } from '@openng/optimus-ui/menu';
import { CheckboxModule } from '@openng/optimus-ui/checkbox';
import { FormsModule } from '@angular/forms';
import { TableOverrides } from './table-overrides';
import { RowOverrides } from './row-overrides';
import { InputTextModule } from '@openng/optimus-ui/inputtext';
import { InputNumberModule } from '@openng/optimus-ui/inputnumber';
import { SelectModule } from '@openng/optimus-ui/select';
import { Column, ColumnUtils } from './column';
import { DatePickerModule } from '@openng/optimus-ui/datepicker';
import { InputMaskModule } from '@openng/optimus-ui/inputmask';
import { FilterOverrides } from './filter-overrides';
import { ButtonModule } from '@openng/optimus-ui/button';
import { ListUtils } from '../../../utils/list-utils';
import { ColumnOverrides } from './column-overrides';
import { SkeletonModule } from '@openng/optimus-ui/skeleton';

interface TableColumn {
  originalColumn: Column;
  frozen?: 'left' | 'right';
}

@Component({
  imports: [
    TableModule,
    MenuModule,
    CheckboxModule,
    FormsModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    DatePickerModule,
    InputMaskModule,
    ButtonModule,
    SkeletonModule,
  ],
  selector: 'pro-legacy-table',
  styleUrl: './table.component.css',
  templateUrl: './table.component.html',
  providers: [FilterService],
})
export class TableComponent {
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
  protected tableColumns: TableColumn[] = [];
  protected tableValue: any[] = [];
  protected tableDefaultSort: SortMeta[] | undefined = undefined;
  protected tableSelection: (any | undefined) | any[] = undefined;
  protected readonly tableDataKey = computed(() => {
    return this.getRowId() != null ? this.rowIdField : undefined;
  });
  protected readonly tableCheckboxSelection = computed(() => {
    return this.selectionMode() === 'multiple' && this.checkboxSelection();
  });

  protected readonly tableRowSelectable = (row: { data: any; index: number }) =>
    row?.data != null && row.data[this.deletedMessageField] == null;

  protected readonly FilterMatchMode = FilterMatchMode;
  protected readonly CustomFilterMatchMode = CustomFilterMatchMode;
  protected readonly ColumnUtils = ColumnUtils;
  protected readonly rowIdField = '__rowId';
  protected readonly deletedMessageField = '__deletedMessage';
  protected readonly indicatorColumnField = '__indicator';

  protected readonly indicatorMenuOptions: MenuItem[] = [
    {
      label: 'Сбросить все сортировки',
      icon: 'pi pi-sort-alt-slash',
      command: () => this.resetAllSorts(),
    },
    {
      label: 'Очистить все фильтры',
      icon: 'pi pi-filter-slash',
      command: () => this.resetAllFilters(),
    },
    {
      label: 'Сбросить все настройки',
      icon: 'pi pi-refresh',
      command: () => this.resetTable(),
    },
  ];

  protected columnMenuOptions: { [key: string]: MenuItem[] } = {};
  private readonly freezeLeftMenuOptionId = 'freezeLeft';
  private readonly freezeRightMenuOptionId = 'freezeLeft';
  private readonly unfreezeMenuOptionId = 'freezeLeft';

  private readonly renderer = inject(Renderer2);
  private readonly filterService = inject(FilterService);
  private readonly table = viewChild.required(Table);
  private readonly rows = viewChildren(SelectableRow);
  private readonly reorderableColumns = viewChildren(ReorderableColumn);
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

  constructor() {
    effect(() => {
      const table = this.table();
      if (table == null) {
        return;
      }

      table.onColumnDrop = TableOverrides.onColumnDrop.bind(table);
      table.onColumnDragLeave = TableOverrides.onColumnDragLeave.bind(table);
      table._filter = TableOverrides._filter(
        (field: string) => this.columnsByField[field]?.originalColumn,
      );
    });

    effect(() => {
      for (let reorderableColumn of this.reorderableColumns()) {
        reorderableColumn.onChanges =
          ColumnOverrides.onReorderableColumnChanges.bind(reorderableColumn);
      }
    });

    effect(() => {
      for (let columnFilter of this.columnFilters()) {
        columnFilter.onRowMatchModeChange = FilterOverrides.onRowMatchModeChange.bind(columnFilter);
      }
    });

    effect(() => {
      for (let row of this.rows()) {
        row.onArrowDownKey = RowOverrides.selectableRowOnArrowDownKey.bind(row);
        row.onArrowUpKey = RowOverrides.selectableRowOnArrowUpKey.bind(row);
        row.onEnterKey = RowOverrides.selectableRowOnEnterKey.bind(row);
        row.onEndKey = RowOverrides.selectableRowOnEndKey.bind(row);
        row.onHomeKey = RowOverrides.selectableRowOnHomeKey.bind(row);
        row.onSpaceKey = RowOverrides.selectableRowOnSpaceKey(() => this.clearSelection()).bind(
          row,
        );
        row.findNextSelectableRow = RowOverrides.findNextSelectableRow.bind(row);
        row.findPrevSelectableRow = RowOverrides.findPrevSelectableRow.bind(row);
      }
    });

    afterRenderEffect(() => {
      const table = this.table();
      if (table == null) {
        return;
      }

      const headerRowHeight = untracked(() => this.headerRowHeight());
      const filterRowHeight = untracked(() => this.filterRowHeight());
      const filter = untracked(() => this.filter());

      this.headerBackgroundListeners.forEach(remove => remove());
      this.headerBackgroundListeners = [];

      if (this.headerBackground != null) {
        try {
          this.renderer.removeChild(this.headerBackground.parentElement, this.headerBackground);
        } finally {
          this.headerBackground = undefined;
        }
      }

      const scroller = this.findScroller();

      if (scroller == null) {
        return;
      }

      this.headerBackground = this.renderer.createElement('div') as HTMLDivElement;
      this.renderer.addClass(this.headerBackground, 'header-background');
      this.recalculateHeaderBackgroundHeight(filter, headerRowHeight, filterRowHeight);

      this.headerBackgroundListeners = [
        this.renderer.listen(this.headerBackground, 'dragenter', (event: DragEvent) =>
          this.onHeaderBackgroundDragEnter(event),
        ),
        this.renderer.listen(this.headerBackground, 'dragover', (event: DragEvent) =>
          this.onHeaderBackgroundDragOver(event),
        ),
        this.renderer.listen(this.headerBackground, 'dragleave', (_: DragEvent) =>
          this.onHeaderBackgroundDragLeave(),
        ),
        this.renderer.listen(this.headerBackground, 'drop', (event: DragEvent) =>
          this.onHeaderBackgroundDrop(event),
        ),
      ];
      this.renderer.insertBefore(scroller, this.headerBackground, scroller.firstChild);
    });

    afterRenderEffect(() => {
      const filter = this.filter();
      const headerRowHeight = this.headerRowHeight();
      const filterRowHeight = this.filterRowHeight();

      this.recalculateHeaderBackgroundHeight(filter, headerRowHeight, filterRowHeight);
    });

    effect(() => {
      this.resetColumns();
    });

    effect(() => {
      const defaultSort = this.defaultSort();
      if (defaultSort != null) {
        this.tableDefaultSort = [...defaultSort];
      } else {
        this.tableDefaultSort = undefined;
      }
    });

    effect(() => {
      const value = this.value();
      this.tableValue = [...(value ?? [])].map(v => this.defineRowId(v));
      this.tableSelection = this.selectionMode() === 'multiple' ? [] : undefined;
      this.selectionChange.emit(this.tableSelection);
    });

    effect(() => {
      const selection = this.selection();
      this.tableSelection =
        this.selectionMode() === 'multiple'
          ? [...(this.selection() ?? [])].map(v => this.defineRowId(v))
          : this.defineRowId(selection);
    });

    this.filterService.register(
      CustomFilterMatchMode.NULLABLE_EQUALS,
      CustomFilterMatchMode.nullableEqualsFilter(
        this.filterService.filters[FilterMatchMode.EQUALS],
      ),
    );
    this.filterService.register(
      CustomFilterMatchMode.NULLABLE_NOT_EQUALS,
      CustomFilterMatchMode.nullableNotEqualsFilter(
        this.filterService.filters[FilterMatchMode.NOT_EQUALS],
      ),
    );

    inject(DestroyRef).onDestroy(() => {
      this.headerBackgroundListeners.forEach(remove => remove());
    });
  }

  protected checkEvent(event?: any) {
    console.log(event ?? 'wtf');
  }

  private defineRowId(row: any): any {
    if (row == null) {
      return row;
    }

    const getRowId = this.getRowId();
    if (getRowId == null) {
      return row;
    }

    Object.defineProperty(row, this.rowIdField, {
      get: () => getRowId(row),
      enumerable: false,
      configurable: true,
    });

    return row;
  }

  private findScroller() {
    return DomHandler.findSingle(
      this.table().el.nativeElement,
      '.p-virtualscroller',
    ) as HTMLElement;
  }

  private recalculateHeaderBackgroundHeight(
    filter: boolean,
    headerRowHeight: number,
    filterRowHeight: number,
  ) {
    let backgroundHeaderHeight: number;
    if (filter) {
      backgroundHeaderHeight = filterRowHeight + headerRowHeight;
    } else {
      backgroundHeaderHeight = headerRowHeight;
    }

    this.renderer.setStyle(this.headerBackground, 'height', `${backgroundHeaderHeight}px`);
    this.renderer.setStyle(this.headerBackground, 'margin-bottom', `-${backgroundHeaderHeight}px`);
  }

  public resetColumns() {
    const columns = [this.indicatorColumn, ...(this.columns() ?? [])];
    const frozenLeftColumns: TableColumn[] = [];
    const notFrozenColumns: TableColumn[] = [];
    const frozenRightColumns: TableColumn[] = [];
    this.columnsByField = {};
    this.columnMenuOptions = {};
    for (let column of columns) {
      const tableColumn = {
        originalColumn: column,
        frozen: column.frozen,
      };
      this.columnsByField[column.field] = tableColumn;
      const menuOptions: MenuItem[] = [
        {
          label: 'Сбросить сортировку',
          icon: 'pi pi-sort-alt-slash',
          command: () => this.resetColumnSort(tableColumn),
        },
        {
          label: 'Очистить фильтр',
          icon: 'pi pi-filter-slash',
          command: () => this.resetColumnFilter(tableColumn),
        },
      ];

      this.generateFreezeColumMenuOptions(tableColumn, menuOptions);

      if (column.frozen === 'left') {
        frozenLeftColumns.push(tableColumn);
      } else if (column.frozen === 'right') {
        frozenRightColumns.push(tableColumn);
      } else {
        notFrozenColumns.push(tableColumn);
      }

      this.columnMenuOptions[column.field] = menuOptions;
    }
    this.tableColumns = [...frozenLeftColumns, ...notFrozenColumns, ...frozenRightColumns];

    if (this.table() == null) {
      return;
    }
    const columnWidths = this.tableColumns.map(col => col.originalColumn.width ?? 200);
    const tableWidth = columnWidths.reduce((acc, curr) => acc + curr, 0);
    // @ts-ignore
    this.table()._initialColWidths = columnWidths;
    requestAnimationFrame(() => {
      this.table().setResizeTableWidth(tableWidth + 'px');
      TableOverrides.forceSetTableWidthsInHtml.bind(this.table())(columnWidths);
    });
  }

  public resetTable() {
    this.resetColumns();
    this.resetFilterAndSort();
  }

  private resetColumnSort(column: TableColumn) {
    if (this.table()._multiSortMeta == null) {
      return;
    }

    this.table()._multiSortMeta = this.table()._multiSortMeta!.filter(
      meta => meta.field !== column.originalColumn.field,
    );

    if (this.table()._multiSortMeta!.length === 0) {
      if (this.defaultSort() == null) {
        this.tableValue = [...(this.value() ?? [])].map(v => this.defineRowId(v));
      } else {
        this.tableDefaultSort = [...this.defaultSort()!];
      }
    } else {
      this.table().sortMultiple();
    }
    const sortableColumn = this.sortableColumns().find(
      c => c.field === column.originalColumn.field,
    );
    if (sortableColumn != null) {
      sortableColumn.sorted = false;
      sortableColumn.sortOrder = 'none';
      sortableColumn.cd.detectChanges();
    }

    const sortableColumnIcon = this.sortableColumnIcons().find(
      c => c.field === column.originalColumn.field,
    );
    if (sortableColumnIcon != null) {
      sortableColumnIcon.sortOrder = 0;
      sortableColumnIcon.cd.detectChanges();
    }
  }

  private resetAllSorts() {
    if (this.table()._multiSortMeta == null) {
      return;
    }

    this.table()._multiSortMeta = [];
    if (this.defaultSort() != null) {
      this.tableDefaultSort = [...this.defaultSort()!];
    } else {
      this.tableValue = [...(this.value() ?? [])].map(v => this.defineRowId(v));
    }

    for (const sortableColumn of this.sortableColumns()) {
      sortableColumn.sorted = false;
      sortableColumn.sortOrder = 'none';
      sortableColumn.cd.detectChanges();
    }

    for (const sortableColumnIcon of this.sortableColumnIcons()) {
      sortableColumnIcon.sortOrder = 0;
      sortableColumnIcon.cd.detectChanges();
    }
  }

  private resetAllFilters() {
    for (let field of Object.keys(this.table().filters)) {
      this.table().filters[field] = {
        value: null,
        matchMode: this.getDefaultMatchMode(this.columnsByField[field]),
      };
    }
    this.table()._filter();
  }

  private resetColumnFilter(column: TableColumn) {
    this.table().filters[column.originalColumn.field] = {

    };
    this.table()._filter();
  }

  public resetFilterAndSort() {
    this.table().reset();
    if (this.defaultSort() != null) {
      this.tableDefaultSort = [...this.defaultSort()!];
    }
  }

  private getLastReorderableHeader() {
    const tableHead = DomHandler.findSingle(
      this.table().el.nativeElement,
      '[data-pc-section="thead"]',
    );
    if (tableHead == null) {
      return null;
    }
    const headers = DomHandler.find(
      tableHead,
      'tr > th[preorderablecolumn]:not(.p-datatable-frozen-column)',
    );
    return headers[headers.length - 1];
  }

  private getHeader(column: TableColumn) {
    const tableHead = DomHandler.findSingle(
      this.table().el.nativeElement,
      '[data-pc-section="thead"]',
    );
    if (tableHead == null) {
      return null;
    }
    const headers = DomHandler.find(
      tableHead,
      `tr > th[preorderablecolumn][data-column-field="${column.originalColumn.field}"]`,
    );
    return headers[headers.length - 1];
  }

  protected onHeaderBackgroundDragEnter(event: DragEvent) {
    this.table().onColumnDragEnter(event, this.getLastReorderableHeader());
  }

  protected onHeaderBackgroundDragOver(event: DragEvent) {
    if (this.table().draggedColumn !== this.getLastReorderableHeader()) {
      event.preventDefault();
    }
  }

  protected onHeaderBackgroundDragLeave() {
    (<ElementRef>this.table().reorderIndicatorUpViewChild).nativeElement.style.display = 'none';
    (<ElementRef>this.table().reorderIndicatorDownViewChild).nativeElement.style.display = 'none';
  }

  protected onHeaderBackgroundDrop(event: DragEvent) {
    this.table().onColumnDrop(event, this.getLastReorderableHeader());
    this.table().cd.detectChanges();
  }

  private generateFreezeColumMenuOptions(column: TableColumn, menuOptions: MenuItem[]) {
    ListUtils.removeIf(
      menuOptions,
      item =>
        item.id === this.unfreezeMenuOptionId ||
        item.id === this.freezeLeftMenuOptionId ||
        item.id === this.freezeRightMenuOptionId,
    );

    if (column.frozen != null) {
      menuOptions.push({
        id: this.unfreezeMenuOptionId,
        label: 'Открепить колонку',
        icon: 'pi pi-lock-open',
        command: () => this.updateFrozenColumn(column, undefined),
      });
    }

    if (column.frozen != 'left') {
      menuOptions.push({
        id: this.freezeLeftMenuOptionId,
        label: 'Закрепить колонку слева',
        icon: 'pi pi-lock',
        command: () => this.updateFrozenColumn(column, 'left'),
      });
    }

    if (column.frozen != 'right') {
      menuOptions.push({
        id: this.freezeRightMenuOptionId,
        label: 'Закрепить колонку справа',
        icon: 'pi pi-lock',
        command: () => this.updateFrozenColumn(column, 'right'),
      });
    }
  }

  private updateFrozenColumn(column: TableColumn, alignFrozen: 'left' | 'right' | undefined) {
    const defaultAction = () => {
      column.frozen = alignFrozen;
      const columnMenuOptions = this.columnMenuOptions[column.originalColumn.field] ?? [];
      this.generateFreezeColumMenuOptions(column, columnMenuOptions);
    };

    const thisHeader = this.getHeader(column);
    if (thisHeader == null) {
      defaultAction();
      return;
    }

    switch (alignFrozen ?? column.frozen) {
      case 'left': {
        let lastLeftFrozenColumn: TableColumn | null = null;
        for (let i = 0; i < this.tableColumns.length; i++) {
          if (this.tableColumns[i].frozen === 'left') {
            lastLeftFrozenColumn = this.tableColumns[i];
          } else {
            break;
          }
        }

        if (lastLeftFrozenColumn == null) {
          const firstHeader = this.getHeader(this.tableColumns[0]);

          if (firstHeader != null && firstHeader != thisHeader) {
            this.table().draggedColumn = thisHeader;
            this.table().dropPosition = -1;
            this.table().onColumnDrop(new MouseEvent('click'), firstHeader);
            this.table().cd.detectChanges();
          }
        } else {
          const lastLeftHeader = this.getHeader(lastLeftFrozenColumn);

          if (lastLeftHeader != null && thisHeader != lastLeftHeader) {
            this.table().draggedColumn = thisHeader;
            this.table().dropPosition = 1;
            this.table().onColumnDrop(new MouseEvent('click'), lastLeftHeader);
            this.table().cd.detectChanges();
          }
        }
        break;
      }
      case 'right': {
        let firstRightFrozenColumn: TableColumn | null = null;
        for (let i = this.tableColumns.length - 1; i >= 0; i--) {
          if (this.tableColumns[i].frozen === 'right') {
            firstRightFrozenColumn = this.tableColumns[i];
          } else {
            break;
          }
        }

        if (firstRightFrozenColumn == null) {
          const lastHeader = this.getHeader(this.tableColumns[this.tableColumns.length - 1]);

          if (lastHeader != null && thisHeader != lastHeader) {
            this.table().draggedColumn = thisHeader;
            this.table().dropPosition = 1;
            this.table().onColumnDrop(new MouseEvent('click'), lastHeader);
            this.table().cd.detectChanges();
          }
        } else {
          const firstRightHeader = this.getHeader(firstRightFrozenColumn);

          if (firstRightHeader != null && thisHeader != firstRightHeader) {
            this.table().draggedColumn = thisHeader;
            this.table().dropPosition = -1;
            this.table().onColumnDrop(new MouseEvent('click'), firstRightHeader);
            this.table().cd.detectChanges();
          }
        }
        break;
      }
    }

    defaultAction();
  }

  protected onColumnHeaderContentDragOver(
    event: DragEvent,
    reorderDisabled: boolean,
    columnHeaderContent: HTMLDivElement,
  ) {
    if (!reorderDisabled) {
      this.table().onColumnDragEnter(event, columnHeaderContent.parentElement);
    }
  }

  protected onTableSort(event: SortEvent) {
    if ((event.sortMode ?? []).length == 0) {
    }
    event.data.sort((data1, data2) => {
      for (let sortMeta of event.multiSortMeta ?? []) {
        const column: TableColumn = this.columnsByField[sortMeta.field ?? ''];
        if (column == null) {
          continue;
        }
        let value1;
        let value2;
        switch (column.originalColumn.type) {
          case 'enum': {
            value1 = ColumnUtils.getColumnValueFormatted(data1, column.originalColumn);
            value2 = ColumnUtils.getColumnValueFormatted(data2, column.originalColumn);
            break;
          }
          default: {
            value1 = ColumnUtils.getColumnValue(data1, column.originalColumn);
            value2 = ColumnUtils.getColumnValue(data2, column.originalColumn);
            break;
          }
        }

        const comparison = ObjectUtils.sort(value1, value2, sortMeta.order, undefined);
        if (comparison !== 0) {
          return comparison;
        }
      }
      return 0;
    });
  }

  protected getFilterMatchMode(column: TableColumn) {
    return (<FilterMetadata>this.table().filters[column.originalColumn.field]).matchMode;
  }

  protected getDefaultMatchMode(column: TableColumn) {
    switch (column?.originalColumn?.type) {
      case 'number':
      case 'int':
      case 'currency':
        return FilterMatchMode.EQUALS;
      case 'date':
        return FilterMatchMode.DATE_IS;
      case 'enum':
      case 'boolean':
        return CustomFilterMatchMode.NULLABLE_EQUALS;
      case 'text':
        return FilterMatchMode.CONTAINS;
      default:
        return FilterMatchMode.EQUALS;
    }
  }

  protected getMatchModeOptions(column: TableColumn): SelectItem<string>[] {
    switch (column.originalColumn.type) {
      case 'number':
      case 'int':
      case 'currency':
        return this.numberMatchModeOptions;
      case 'boolean':
        return this.booleanMatchModeOptions;
      case 'text':
        return this.textMatchModeOptions;
      case 'date':
        return this.dateMatchModeOptions;
      case 'enum':
        return this.enumMatchModeOptions;
      default:
        return [];
    }
  }

  protected getEnumEqualsOptions(column: TableColumn): SelectItem[] {
    if (column.originalColumn.type !== 'enum') {
      return [];
    }

    const options = [];
    for (let key of Object.keys(column.originalColumn.locale)) {
      options.push({
        label: column.originalColumn.locale[key],
        value: key,
      });
    }
    options.push({
      label: 'Не указано',
      value: CustomFilterMatchMode.nullReservedFilterWord,
    });

    return options;
  }

  public clearSelection() {
    this.tableSelection = this.selectionMode() === 'multiple' ? [] : undefined;
    this.selectionChange.emit(this.tableSelection);
  }

  public findAndSelectRow(seekedRow: any, smooth = false): Promise<boolean> {
    return new Promise<boolean>(resolve => {
      requestAnimationFrame(() => {
        const getRowId = this.getRowId();
        let filteredData = this.tableValue;
        const seekedRowIndex = filteredData.findIndex(row => {
          if (getRowId != null) {
            return getRowId(seekedRow) === getRowId(row);
          } else {
            return seekedRow === row;
          }
        });

        if (seekedRowIndex == -1) {
          this.clearSelection();
          resolve(false);
          return;
        }

        const scroller = this.findScroller();
        if (scroller == null) {
          this.clearSelection();
          resolve(false);
          return;
        }

        let rowHeight = this.rowHeight();
        const rowTop = seekedRowIndex * rowHeight;
        const rowBottom = (seekedRowIndex + 1) * rowHeight;

        const viewportTop = scroller.scrollTop;
        const viewportBottom = viewportTop + scroller.clientHeight;

        if (rowTop < viewportTop || rowBottom > viewportBottom) {
          const rowsPerViewPort = scroller.clientHeight / rowHeight;
          let scrollToIndex = seekedRowIndex - Math.round(rowsPerViewPort / 2) + 1;

          if (scrollToIndex < 0) {
            scrollToIndex = 0;
          }

          if (smooth) {
            this.table().scroller?.scrollToIndex(scrollToIndex, 'smooth');
          } else {
            this.table().scrollToVirtualIndex(scrollToIndex);
          }
        }

        this.tableSelection =
          this.selectionMode() === 'multiple'
            ? [filteredData[seekedRowIndex]]
            : filteredData[seekedRowIndex];
        this.selectionChange.emit(this.tableSelection);
        resolve(true);
      });
    });
  }

  public updateRows(rows: any[]) {
    const getRowId = this.getRowId();

    if (getRowId == null) {
      this.table().cd.detectChanges();
      return;
    }

    const rowByKey = new Map<string, any>();
    for (let row of rows) {
      rowByKey.set(getRowId(row), row);
    }

    for (let i = 0; i < this.tableValue.length; i++) {
      const tableRow = this.tableValue[i];
      let tableRowId = getRowId(tableRow);
      if (rowByKey.has(tableRowId)) {
        this.tableValue[i] = rowByKey.get(tableRowId);
      }
    }
    this.table().cd.detectChanges();
  }

  public removeRows(rows: any[], message: string) {
    const getRowId = this.getRowId();

    if (getRowId != null) {
      const rowByKey = new Map<string, any>();
      for (let row of rows) {
        rowByKey.set(getRowId(row), row);
      }

      for (let i = 0; i < this.tableValue.length; i++) {
        const tableRow = this.tableValue[i];
        let tableRowId = getRowId(tableRow);
        if (rowByKey.has(tableRowId)) {
          this.tableValue[i][this.deletedMessageField] = message;
        }
      }
    } else {
      for (let i = 0; i < this.tableValue.length; i++) {
        const tableRow = this.tableValue[i];
        if (rows.indexOf(tableRow) != -1) {
          this.tableValue[i][this.deletedMessageField] = message;
        }
      }
    }
    this.table().cd.detectChanges();
  }

  public getTitle(title: string) {
    return `${title} (${this.table()?.processedData?.length ?? 0})`;
  }
}

interface SortEvent {
  data: any[];
  sortMode: 'single' | 'multiple';
  field: string;
  order: number;
  multiSortMeta: SortMeta[];
}

class CustomFilterMatchMode {
  public static readonly nullReservedFilterWord = '__nullReservedWord';
  public static readonly NULLABLE_EQUALS = 'nullableEquals';
  public static readonly NULLABLE_NOT_EQUALS = 'nullableNotEquals';

  public static nullableEqualsFilter(
    defaultEquals: Function,
  ): (value: any, filter: any, filterLocale?: any) => boolean {
    return (value, filter, filterLocale) => {
      if (filter === CustomFilterMatchMode.nullReservedFilterWord) {
        return value == null;
      }
      return defaultEquals(value, filter, filterLocale);
    };
  }

  public static nullableNotEqualsFilter(
    defaultNotEquals: Function,
  ): (value: any, filter: any, filterLocale?: any) => boolean {
    return (value, filter, filterLocale) => {
      if (filter === CustomFilterMatchMode.nullReservedFilterWord) {
        return value != null;
      }
      return defaultNotEquals(value, filter, filterLocale);
    };
  }
}
