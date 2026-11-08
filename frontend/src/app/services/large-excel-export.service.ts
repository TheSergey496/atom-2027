import { Service } from '@angular/core';
import { Column, ColumnUtils } from '../components/library/table/column';
import { DateUtils } from '../utils/date-utils';
import { collectToBlob, createWorkbookWriter } from '@jetstreamapp/simple-excel';
import { ExcelExportService } from './excel-export.service';

@Service()
export class LargeExcelExportService {
  private static readonly MAX_ROWS_PER_SHEET = 1_000_000;
  private static readonly DEFAULT_COLUMN_WIDTH = 22;

  public isSupported(): boolean {
    return !(
      typeof ReadableStream === 'undefined' ||
      typeof WritableStream === 'undefined' ||
      typeof TransformStream === 'undefined' ||
      typeof CompressionStream === 'undefined' ||
      typeof DecompressionStream === 'undefined'
    );
  }

  public async export(
    columns: Column[],
    data: any[],
    fileName = 'export.xlsx',
    pixelWidths?: number[],
  ): Promise<void> {
    if (!columns.length) {
      throw new Error('Cannot export without columns');
    }

    const sink = collectToBlob();

    const workbook = createWorkbookWriter(sink, {
      strings: 'inline',
      cellOverflow: 'truncate',
    });

    const headerStyle = workbook.registerStyle({
      font: {
        bold: true,
        size: 11,
        color: '#000000',
      },
      fill: {
        color: '#F5F8FA',
      },
      alignment: {
        vertical: 'center',
      },
    });

    const dateStyle = workbook.registerStyle({
      numFmt: 14,
    });

    const columnWidths: number[] = [];
    const cellValueGetters: ((row: any) => any)[] = [];
    const cellStyles: any[] = [];
    for (let i = 0; i < columns.length; i++) {
      const column = columns[i];

      const pixelWidth = pixelWidths?.[i];
      columnWidths.push(
        pixelWidth != null
          ? this.pixelsToExcelWidth(pixelWidth)
          : ExcelExportService.DEFAULT_COLUMN_WIDTH,
      );

      let cellValueGetter: (row: any) => any = _ => null;
      switch (column.type) {
        case 'enum': {
          cellValueGetter = (row: any) => ColumnUtils.getColumnValueFormatted(row, column);
          break;
        }
        case 'date': {
          cellValueGetter = (row: any) =>
            DateUtils.toLocalDate(<Date>ColumnUtils.getColumnValue(row, column));
          break;
        }
        default: {
          cellValueGetter = (row: any) => ColumnUtils.getColumnValue(row, column);
          break;
        }
      }
      cellValueGetters.push(cellValueGetter);

      let cellStyle = undefined;
      switch (column.type) {
        case 'date': {
          cellStyle = dateStyle;
          break;
        }
        default: {
          cellStyle = undefined;
          break;
        }
      }
      cellStyles.push(cellStyle);
    }

    const sheetCount = Math.max(
      1,
      Math.ceil(data.length / (ExcelExportService.MAX_ROWS_PER_SHEET - 1)),
    );

    let processedRows = 0;
    try {
      for (let sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
        const start = sheetIndex * (ExcelExportService.MAX_ROWS_PER_SHEET - 1);
        const end = Math.min(start + (ExcelExportService.MAX_ROWS_PER_SHEET - 1), data.length);
        const rowCount = end - start + 1;

        const sheet = workbook.addSheet(`Лист ${sheetIndex + 1}`, {
          header: columns.map(column => column.header),
          headerStyle,
          columns: columnWidths.map(width => ({ width })),
          freeze: {
            rows: 1,
          },
          autoFilter: true,
          rowCount,
        });

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

          await sheet.writeRow(values, cellStyles);
        }

        await sheet.close();
      }

      await workbook.close();

      const blob = await sink.result();

      this.download(blob, fileName);
    } catch (error) {
      await workbook.abort(error);
      throw error;
    }
  }

  private pixelsToExcelWidth(pixels: number): number {
    return Math.max(0, (pixels - 5) / 7);
  }

  private download(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = fileName.endsWith('.xlsx') ? fileName : `${fileName}.xlsx`;

    link.click();

    URL.revokeObjectURL(url);
  }
}
