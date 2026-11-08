import { Service } from '@angular/core';
import ExcelJS from 'exceljs';
import { Column, ColumnUtils } from '../components/library/table/column';
import { DateUtils } from '../utils/date-utils';

@Service()
export class ExcelExportService {
  public static readonly MAX_ROWS_PER_SHEET = 1_000_000;
  public static readonly BATCH_SIZE = 500;
  public static readonly DEFAULT_COLUMN_WIDTH = 22;

  public async export(
    columns: Column[],
    data: any[],
    fileName = 'export.xlsx',
    pixelWidths?: number[],
  ): Promise<void> {
    const workbook = new ExcelJS.Workbook();
    workbook.creator = 'JustWorks';
    workbook.created = new Date();

    const sheetCount = Math.max(
      1,
      Math.ceil(data.length / (ExcelExportService.MAX_ROWS_PER_SHEET - 1)),
    );

    const columnWidths = columns.map((column, index) => {
      const pixelWidth = pixelWidths?.[index];

      return pixelWidth != null
        ? this.pixelsToExcelWidth(pixelWidth)
        : ExcelExportService.DEFAULT_COLUMN_WIDTH;
    });

    const cellValueGetters = columns.map(column => {
      switch (column.type) {
        case 'enum': {
          return (row: any) => ColumnUtils.getColumnValueFormatted(row, column);
        }
        case 'date': {
          return (row: any) => DateUtils.toLocalDate(<Date>ColumnUtils.getColumnValue(row, column));
        }
        default: {
          return (row: any) => ColumnUtils.getColumnValue(row, column);
        }
      }
    });

    let processedRows = 0;

    for (let sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
      const worksheet = workbook.addWorksheet(`Лист ${sheetIndex + 1}`);

      this.configureWorksheet(worksheet, columns, columnWidths);

      const start = sheetIndex * (ExcelExportService.MAX_ROWS_PER_SHEET - 1);
      const end = Math.min(start + (ExcelExportService.MAX_ROWS_PER_SHEET - 1), data.length);

      for (let y = start; y < end; y++) {
        processedRows++;
        if (processedRows >= ExcelExportService.BATCH_SIZE) {
          await new Promise<void>(resolve => setTimeout(resolve));
          processedRows = 0;
        }
        const row = data[y];
        const values = new Array(columns.length);

        for (let x = 0; x < cellValueGetters.length; x++) {
          values[x] = cellValueGetters[x](row);
        }

        worksheet.addRow(values);
      }
    }

    const buffer = await workbook.xlsx.writeBuffer();

    this.download(buffer, fileName);
  }

  private pixelsToExcelWidth(pixels: number): number {
    return Math.max(0, (pixels - 5) / 7);
  }

  private configureWorksheet(
    worksheet: ExcelJS.Worksheet,
    columns: Column[],
    columnWidths: number[],
  ): void {
    worksheet.columns = columns.map((column, index) => ({
      key: `column${index}`,
      width: columnWidths[index]
    }));

    const headers = columns.map(column => column.header);

    const headerRow = worksheet.addRow(headers);

    headerRow.eachCell(cell => {
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'F5F8FA' },
      };

      cell.font = {
        bold: true,
        size: 11,
      };

      cell.alignment = {
        vertical: 'middle',
        horizontal: 'left',
      };
    });

    worksheet.views = [
      {
        state: 'frozen',
        ySplit: 1,
      },
    ];

    if (columns.length > 0) {
      worksheet.autoFilter = {
        from: { row: 1, column: 1 },
        to: { row: 1, column: columns.length },
      };
    }
  }

  private download(buffer: ExcelJS.Buffer, fileName: string): void {
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = fileName;
    link.click();

    URL.revokeObjectURL(url);
  }
}
