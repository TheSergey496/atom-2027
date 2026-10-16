import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Service, inject} from '@angular/core';
import {RoleDto, UserDto} from './auth-dto';
import {PageQuery, PageResponse} from './query-lazy';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Service()
export class UserAdminService {
  httpService: HttpClient;


 public archiveUser(userId: number): Observable<UserDto>  {
    return this.httpService.delete<UserDto>('administration/users/' + userId + '/archive', {responseType: 'json'});
  }

 public constructor() {
    this.httpService = inject(HttpClient);
  }

 public createUser(userDto: UserDto): Observable<UserDto>  {
    const headers = new HttpHeaders().set('Content-type', 'application/json');
    return this.httpService.post<UserDto>('administration/users', JSON.stringify(userDto) , {headers, responseType: 'json'});
  }

 public getRoles(): Observable<RoleDto[]>  {
    return this.httpService.get<RoleDto[]>('administration/users/roles', {responseType: 'json'});
  }

 public getUser(userId: number, joinRoles: boolean = false): Observable<UserDto>  {
    const queryParamsList: { name: string, value: string }[] = [];
    if (joinRoles !== undefined && joinRoles !== null) {
      queryParamsList.push({name: 'joinRoles', value: joinRoles.toString()});
    }
    let params = new HttpParams();
    for (const queryParam of queryParamsList) {
      params = params.append(queryParam.name, queryParam.value);
    }
    return this.httpService.get<UserDto>('administration/users/' + userId + '', {params, responseType: 'json'});
  }

 public getUsers(joinRoles: boolean = false): Observable<UserDto[]>  {
    const queryParamsList: { name: string, value: string }[] = [];
    if (joinRoles !== undefined && joinRoles !== null) {
      queryParamsList.push({name: 'joinRoles', value: joinRoles.toString()});
    }
    let params = new HttpParams();
    for (const queryParam of queryParamsList) {
      params = params.append(queryParam.name, queryParam.value);
    }
    return this.httpService.get<UserDto[]>('administration/users', {params, responseType: 'json'});
  }

 public resetPassword(userId: number): Observable<UserDto>  {
    return this.httpService.patch<UserDto>('administration/users/' + userId + '', null , {responseType: 'json'});
  }

 public searchUsers(pageQuery: PageQuery): Observable<PageResponse<UserDto> >  {
    const headers = new HttpHeaders().set('Content-type', 'application/json');
    return this.httpService.post<PageResponse<UserDto> >('administration/users/search', JSON.stringify(pageQuery) , {headers, responseType: 'json'});
  }

 public unarchiveUser(userId: number): Observable<UserDto>  {
    return this.httpService.put<UserDto>('administration/users/' + userId + '/archive', null , {responseType: 'json'});
  }

 public updateUser(userId: number, userDto: UserDto): Observable<UserDto>  {
    const headers = new HttpHeaders().set('Content-type', 'application/json');
    return this.httpService.put<UserDto>('administration/users/' + userId + '', JSON.stringify(userDto) , {headers, responseType: 'json'});
  }

}

