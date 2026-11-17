import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Service, inject} from '@angular/core';
import {EdificeDependencyDto, EdificeDto, EventDto} from './atom2027-dto';
import {TestQueue$TestQueuePayload} from './atom2027-queue';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Service()
export class EdificeDependencyService {
  httpService: HttpClient;


 public constructor() {
    this.httpService = inject(HttpClient);
  }

 public getEdificeDependencies(): Observable<EdificeDependencyDto[]>  {
    return this.httpService.get<EdificeDependencyDto[]>('edifice-dependencies', {responseType: 'json'});
  }

}

@Service()
export class EdificeService {
  httpService: HttpClient;


 public constructor() {
    this.httpService = inject(HttpClient);
  }

 public getEdifices(instantAt?: Date): Observable<EdificeDto[]>  {
    const queryParamsList: { name: string, value: string }[] = [];
    if (instantAt !== undefined && instantAt !== null) {
      queryParamsList.push({name: 'instantAt', value: instantAt.toString()});
    }
    let params = new HttpParams();
    for (const queryParam of queryParamsList) {
      params = params.append(queryParam.name, queryParam.value);
    }
    return this.httpService.get<EdificeDto[]>('edifices', {params, responseType: 'json'});
  }

}

@Service()
export class EventService {
  httpService: HttpClient;


 public constructor() {
    this.httpService = inject(HttpClient);
  }

 public getEvents(edificeId?: number): Observable<EventDto[]>  {
    const queryParamsList: { name: string, value: string }[] = [];
    if (edificeId !== undefined && edificeId !== null) {
      queryParamsList.push({name: 'edificeId', value: edificeId.toString()});
    }
    let params = new HttpParams();
    for (const queryParam of queryParamsList) {
      params = params.append(queryParam.name, queryParam.value);
    }
    return this.httpService.get<EventDto[]>('events', {params, responseType: 'json'});
  }

}

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

