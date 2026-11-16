import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
  viewChildren,
} from '@angular/core';
import { Column, ColumnUtils, TableColumn } from '../column';
import {
  FilterMatchMode,
  FilterMetadata,
  FilterService,
  MenuItem,
  MessageService,
  SelectItem,
  SortMeta,
  TreeNode,
} from '@openng/optimus-ui/api';
import { ExcelExportService } from '../../../../services/excel-export.service';
import { LargeExcelExportService } from '../../../../services/large-excel-export.service';
import { TableFilterEvent } from '@openng/optimus-ui/table';
import { CustomFilterMatchMode } from '../custom-filter-match-mode';
import {
  TreeTable,
  TreeTableModule,
  TreeTableSortEvent,
  TTRow,
} from '@openng/optimus-ui/treetable';
import { DomHandler } from '@openng/optimus-ui/dom';
import { ObjectUtils } from '@openng/optimus-ui/utils';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TableUtils } from '../table-utils';
import { ButtonDirective, ButtonIcon } from '@openng/optimus-ui/button';
import { Menu } from '@openng/optimus-ui/menu';
import { CheckboxModule } from '@openng/optimus-ui/checkbox';
import { TreeTableOverrides } from './tree-table-overrides';
import { RowOverrides } from './row-overrides';

@Component({
  imports: [
    TreeTableModule,
    ReactiveFormsModule,
    ButtonDirective,
    ButtonIcon,
    Menu,
    FormsModule,
    CheckboxModule,
  ],
  selector: 'pro-legacy-tree-table',
  styleUrl: './tree-table.component.css',
  templateUrl: './tree-table.component.html',
})
export class TreeTableComponent {
  public readonly columns = input<Column[]>([]);
  public readonly value = input<TreeNode[]>([]);
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
  protected tableValue: TreeNode[] = [];
  protected tableDefaultSort: SortMeta[] | undefined = undefined;
  protected tableSelection: (any | undefined) | any[] = undefined;
  protected readonly tableDataKey = computed(() => {
    return this.getRowId() != null ? this.rowIdField : undefined;
  });

  protected tableColumns: TableColumn[] = [];
  protected tableScrollableColumns: TableColumn[] = [];
  protected tableFrozenColumns: TableColumn[] = [];
  protected tableGlobalFilterFields: string[] = [];

  protected readonly indicatorColumnField = '__indicator';
  private readonly indicatorColumn: Column = {
    field: this.indicatorColumnField,
    header: '',
    type: 'text',
    width: 32,
    frozen: 'left',
    getter: () => undefined,
  };

  protected readonly blockingMask = signal<boolean>(false);
  protected readonly tableHasFilter = signal<boolean>(false);
  protected tableAppliedFilters: { column: TableColumn; label: string; valueLocale: string }[] = [];

  protected readonly FilterMatchMode = FilterMatchMode;
  protected readonly CustomFilterMatchMode = CustomFilterMatchMode;
  protected readonly ColumnUtils = ColumnUtils;
  protected readonly rowIdField = '__rowId';

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

  private readonly filterService = inject(FilterService);
  private readonly excelExportService = inject(ExcelExportService);
  private readonly largeExcelExportService = inject(LargeExcelExportService);
  private readonly messageService = inject(MessageService);
  private readonly table = viewChild.required(TreeTable);
  private readonly rows = viewChildren(TTRow);

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
      table.onColumnResizeEnd = TreeTableOverrides.onColumnResizeEnd.bind(table);
    });

    effect(() => {
      for (let row of this.rows()) {
        row.onArrowUpKey = RowOverrides.onArrowUpKey.bind(row);
        row.onArrowDownKey = RowOverrides.onArrowDownKey.bind(row);
      }
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
      this.defineRowIdForTreeNodes(value);
      this.tableValue = [...(value ?? [])];
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
  }

  protected onFilter(event: TableFilterEvent) {
    this.clearSelection();
    const filterCalc = TableUtils.generateAppliedFilters(
      event,
      this.numberFormatters,
      this.columnsByField,
    );

    this.tableHasFilter.set(filterCalc.hasFilter);
    this.tableAppliedFilters = filterCalc.appliedFilters;
  }

  private defineRowIdForTreeNodes(nodes: TreeNode[]): any {
    const queue = [...(nodes ?? [])];
    while (queue.length > 0) {
      const node = queue.shift();
      this.defineRowId(node?.data);
      queue.push(...(node?.children ?? []));
    }
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

  public resetColumns() {
    const columns = [this.indicatorColumn, ...(this.columns() ?? [])];
    const tableColumns: TableColumn[] = [];
    const tableScrollableColumns: TableColumn[] = [];
    const tableFrozenColumns: TableColumn[] = [];
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

      tableColumns.push(tableColumn);
      if (column.field === this.indicatorColumnField) {
        tableFrozenColumns.push(tableColumn);
      } else {
        tableScrollableColumns.push(tableColumn);
      }

      this.columnMenuOptions[column.field] = menuOptions;
    }
    this.tableColumns = tableColumns;
    this.tableFrozenColumns = tableFrozenColumns;
    this.tableScrollableColumns = tableScrollableColumns;
    this.tableGlobalFilterFields = this.tableColumns.map(c => c.originalColumn.field);

    if (this.table() == null) {
      return;
    }
    const columnWidths = this.tableScrollableColumns.map(col => col.originalColumn.width ?? 200);
    requestAnimationFrame(() => {
      TreeTableOverrides.forceSetTableWidthsInHtml.call(this.table(), columnWidths);
    });
  }

  public resetTable() {
    this.resetColumns();
    this.resetAllSorts();
    this.resetAllFilters();
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
        this.defineRowIdForTreeNodes(this.value());
        this.tableValue = [...(this.value() ?? [])];
      } else {
        this.tableDefaultSort = [...this.defaultSort()!];
      }
    } else {
      this.table().sortMultiple();
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
      this.defineRowIdForTreeNodes(this.value());
      this.tableValue = [...(this.value() ?? [])];
    }
  }

  private resetAllFilters() {
    for (let field of Object.keys(this.table().filters)) {
      if (field === 'global') {
        continue;
      }
      this.table().filters[field] = {
        value: null,
        matchMode: this.getDefaultMatchMode(this.columnsByField[field]),
      };
    }
    this.table()._filter();
  }

  protected resetColumnFilter(column: TableColumn) {
    this.table().filters[column.originalColumn.field] = {
      matchMode: this.getDefaultMatchMode(column),
    };
    this.table()._filter();
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

  protected onColumnHeaderContentDragOver(
    event: DragEvent,
    reorderDisabled: boolean,
    columnHeaderContent: HTMLDivElement,
  ) {
    if (!reorderDisabled) {
      this.table().onColumnDragEnter(event, columnHeaderContent.parentElement);
    }
  }

  protected onTableSort(event: TreeTableSortEvent) {
    if ((event.multiSortMeta ?? []).length == 0) {
    }
    const queue = [event.data ?? []];
    while (queue.length > 0) {
      const data = <TreeNode[]>queue.shift();
      data.sort((node1, node2) => {
        const data1 = node1?.data;
        const data2 = node2?.data;
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

      for (let node of data) {
        if (node.children != null && node.children.length > 0) {
          queue.push(node.children);
        }
      }
    }
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

  public applyGlobalFilter(value: string) {
    this.table().filterGlobal(value, FilterMatchMode.CONTAINS);
  }
}
