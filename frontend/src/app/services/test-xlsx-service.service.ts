import { inject, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Service()
export class TestXlsxServiceService {
  private readonly httpClient = inject(HttpClient);

  public foo(): Observable<Blob> {
    return this.httpClient.post('xlsx/test', null, { responseType: 'blob' });
  }
}
