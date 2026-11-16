import { inject, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Service()
export class ReportService {
  private readonly httpClient = inject(HttpClient);

  public testReport(): Observable<Blob> {
    return this.httpClient.get(`reports/test`, { responseType: 'blob' });
  }
}
