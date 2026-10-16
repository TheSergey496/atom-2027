import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Service, inject} from '@angular/core';
import {TestQueue$TestQueuePayload} from './atom2027-queue';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Service()
export class LogService {
  httpService: HttpClient;


 public constructor() {
    this.httpService = inject(HttpClient);
  }

}

@Service()
export class QueueTestService {
  httpService: HttpClient;


 public constructor() {
    this.httpService = inject(HttpClient);
  }

 public postMessage(payload: TestQueue$TestQueuePayload): Observable<void>  {
    const headers = new HttpHeaders().set('Content-type', 'application/json');
    return this.httpService.post<void>('queue-test', JSON.stringify(payload) , {headers});
  }

}

