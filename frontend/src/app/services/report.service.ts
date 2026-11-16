import { inject, Service } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Service()
export class ReportService {
  private readonly httpClient = inject(HttpClient);

  public testReport(format: ExportFormat): Observable<Blob> {
    const queryParamsList: { name: string; value: string }[] = [];
    if (format != null) {
      queryParamsList.push({ name: 'exportFormat', value: format });
    }
    let params = new HttpParams();
    for (const queryParam of queryParamsList) {
      params = params.append(queryParam.name, queryParam.value);
    }
    return this.httpClient.get(`reports/test`, { params, responseType: 'blob' });
  }
}

export enum ExportFormat {
  PDF = 'PDF',
  XLSX = 'XLSX',
  DOCX = 'DOCX'
}
