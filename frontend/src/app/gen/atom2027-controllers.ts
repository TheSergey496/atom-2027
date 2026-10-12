import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Service, inject} from '@angular/core';
import {TestQueue$TestQueuePayload} from './atom2027-services';
import {UserDto} from './auth-dto';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Service()
export class AdminService {
  httpService: HttpClient;


 public constructor() {
    this.httpService = inject(HttpClient);
  }

 public getUsers(joinRoles: boolean): Observable<UserDto[]>  {
    const queryParamsList: { name: string, value: string }[] = [];
    queryParamsList.push({name: 'joinRoles', value: joinRoles.toString()});
      let params = new HttpParams();
    for (const queryParam of queryParamsList) {
      params = params.append(queryParam.name, queryParam.value);
    }
    return this.httpService.get<UserDto[]>('admin/users', {params, responseType: 'json'});
  }

 public test(): Observable<void>  {
    return this.httpService.get<void>('admin');
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

