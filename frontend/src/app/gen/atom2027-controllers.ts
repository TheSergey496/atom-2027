import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { UserDto } from './auth-dto';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  httpService: HttpClient;

  public constructor(httpService: HttpClient) {
    this.httpService = httpService;
  }

  public getUsers(joinRoles: boolean): Observable<UserDto[]> {
    const queryParamsList: { name: string; value: string }[] = [];
    queryParamsList.push({ name: 'joinRoles', value: joinRoles.toString() });
    let params = new HttpParams();
    for (const queryParam of queryParamsList) {
      params = params.append(queryParam.name, queryParam.value);
    }
    return this.httpService.get<UserDto[]>('admin/users', { params, responseType: 'json' });
  }

  public test(): Observable<void> {
    return this.httpService.get<void>('admin');
  }
}
