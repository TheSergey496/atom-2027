import { getField } from '../../../utils/field-accessor';

interface BaseColumn {
  header: string;
  field: string;
  width?: number;
  frozen?: 'left' | 'right';
}

export type Column =
  | (BaseColumn & {
      type: 'text';
      getter?: (row: any) => string | undefined;
      formatter?: (value: string | undefined) => string;
    })
  | (BaseColumn & {
      type: 'date';
      getter?: (row: any) => Date | undefined;
      formatter?: (value: Date | undefined) => string;
    })
  | (BaseColumn & {
      type: 'int';
      getter?: (row: any) => number | undefined;
      formatter?: (value: number | undefined) => string;
    })
  | (BaseColumn & {
      type: 'number';
      minFractionDigits?: number;
      maxFractionDigits?: number;
      getter?: (row: any) => number | undefined;
      formatter?: (value: number | undefined) => string;
    })
  | (BaseColumn & {
      type: 'currency';
      getter?: (row: any) => number | undefined;
      formatter?: (value: number | undefined) => string;
    })
  | (BaseColumn & {
      type: 'boolean';
      allowEmpty?: boolean;
      getter?: (row: any) => boolean | undefined;
    })
  | (BaseColumn & {
      type: 'enum';
      getter?: (row: any) => any | undefined;
      locale: { [key: string]: string };
    });

export interface TableColumn {
  originalColumn: Column;
  frozen?: 'left' | 'right';
}

export class ColumnUtils {
  public static getColumnValue(
    row: any,
    column: Extract<Column, { type: 'text' }>,
  ): string | undefined;
  public static getColumnValue(
    row: any,
    column: Extract<Column, { type: 'date' }>,
  ): Date | undefined;
  public static getColumnValue(
    row: any,
    column: Extract<Column, { type: 'number' }>,
  ): number | undefined;
  public static getColumnValue(
    row: any,
    column: Extract<Column, { type: 'boolean' }>,
  ): boolean | undefined;
  public static getColumnValue(
    row: any,
    column: Extract<Column, { type: 'enum' }>,
  ): any | undefined;
  public static getColumnValue(
    row: any,
    column: Column,
  ): number | string | boolean | Date | any | undefined;

  public static getColumnValue(
    row: any,
    column: Column,
  ): number | string | boolean | Date | any | undefined {
    if (column == null) {
      return null;
    }
    let value = null;
    if (column.getter != null) {
      value = column.getter(row);
    } else {
      value = getField(row, column.field);
    }

    switch (column.type) {
      case 'int':
      case 'currency':
      case 'number': {
        let numberValue = value as number | undefined;
        if (Number.isNaN(numberValue)) {
          return undefined;
        }
        return numberValue;
      }
      case 'text': {
        return value as string | undefined;
      }
      case 'date': {
        if (value != null) {
          if (typeof value === 'string' || value instanceof String) {
            value = new Date((value as string | String).toString());
          }
        }

        return value as Date | undefined;
      }
      case 'boolean': {
        let booleanValue = value as boolean | undefined;
        if (booleanValue == null && column.allowEmpty !== true) {
          booleanValue = false;
        }
        return booleanValue;
      }
      case 'enum':
        return value;
    }
  }

  public static getNumberFormatter(
    numberFormatterCache: { [key: string]: Intl.NumberFormat } | undefined,
    minFractionDigits: number | undefined,
    maxFractionDigits: number | undefined,
    useGrouping: boolean | undefined,
  ) {
    const key = `${minFractionDigits}/${maxFractionDigits}/${useGrouping ?? false}`;
    let formatter = numberFormatterCache != null ? numberFormatterCache[key] : null;

    if (formatter != null) {
      return formatter;
    }

    const formatterOptions: { [key: string]: any } = {
      notation: 'standard',
      useGrouping: useGrouping ?? false,
    };
    if (minFractionDigits != null) {
      formatterOptions['minimumFractionDigits'] = minFractionDigits;
    }
    if (maxFractionDigits != null) {
      formatterOptions['maximumFractionDigits'] = maxFractionDigits;
    }
    formatter = new Intl.NumberFormat('ru-RU', formatterOptions);
    if (numberFormatterCache != null) {
      numberFormatterCache[key] = formatter;
    }
    return formatter;
  }

  public static getColumnValueFormatted(
    row: any,
    column: Column,
    numberFormatterCache?: { [key: string]: Intl.NumberFormat },
  ): string {
    if (column == null) {
      return '';
    }
    switch (column.type) {
      case 'int':
      case 'currency':
      case 'number': {
        const numberValue = ColumnUtils.getColumnValue(row, column);
        if (column.formatter != null) {
          return column.formatter(numberValue);
        } else if (numberValue == null) {
          return '';
        } else {
          let minFractionDigits: number | undefined;
          let maxFractionDigits: number | undefined;
          let useGrouping: boolean;
          switch (column.type) {
            case 'number': {
              minFractionDigits = column.minFractionDigits ?? undefined;
              maxFractionDigits = column.maxFractionDigits ?? 20;
              useGrouping = true;
              break;
            }
            case 'int':
              minFractionDigits = 0;
              maxFractionDigits = 0;
              useGrouping = false;
              break;
            case 'currency':
              minFractionDigits = 2;
              maxFractionDigits = 2;
              useGrouping = true;
              break;
          }

          const formatter = ColumnUtils.getNumberFormatter(
            numberFormatterCache,
            minFractionDigits,
            maxFractionDigits,
            useGrouping,
          );
          return formatter.format(numberValue);
        }
      }
      case 'text': {
        const textValue = ColumnUtils.getColumnValue(row, column);
        if (column.formatter != null) {
          return column.formatter(textValue);
        } else {
          return textValue ?? '';
        }
      }
      case 'date': {
        const dateValue = ColumnUtils.getColumnValue(row, column);
        if (dateValue != null && Number.isNaN(dateValue.getTime())) {
          return 'Некорректная дата';
        }

        if (column.formatter != null) {
          return column.formatter(dateValue);
        } else {
          return dateValue != null
            ? dateValue.toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
              })
            : '';
        }
      }
      case 'enum': {
        const enumValue = ColumnUtils.getColumnValue(row, column);
        if (enumValue == null) {
          return '';
        }
        return column.locale[enumValue] ?? '';
      }
      default: {
        return '';
      }
    }
  }
}
