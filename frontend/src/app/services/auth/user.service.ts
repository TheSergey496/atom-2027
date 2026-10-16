import { inject, Service, signal } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { TokenStorageService } from './tokenStorage.service';
import { UserDto } from '../../gen/auth-dto';
import { hashAndBase64 } from '../../utils/hash';
import { Router } from '@angular/router';

@Service()
export class UserService {
  private readonly authRestPath = 'authentication';
  private readonly userRestPath = 'user';
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  private readonly auth = signal<UserDto | null>(null);

  public async init() {
    let user = await this.getUserByToken();
    if (user != null) {
      this.auth.set(user);
    } else {
      await this.router.navigate(['/login']);
    }
  }

  public get currentUser(): UserDto | null {
    return this.auth();
  }

  public async login(email: string, password: string): Promise<UserDto | null> {
    const response: HttpResponse<UserDto> = await firstValueFrom(
      this.http.post<UserDto>(
        this.authRestPath,
        {
          email: email,
          password: await hashAndBase64(password),
        },
        { observe: 'response' },
      ),
    );

    if (response.body == null) {
      return null;
    }

    this.tokenStorage.saveAccessToken(
      response.headers.get('Authorization'),
      response.headers.get('Expires'),
    );
    this.auth.set(response.body);
    return response.body;
  }

  public async logout() {
    await firstValueFrom(this.http.delete(this.authRestPath));
    this.auth.set(null);
    this.tokenStorage.deleteAccessToken();
    await this.router.navigate(['/login']);
  }

  public async registerUser(user: UserDto): Promise<void> {
    const userWithEncodedPassword = { ...user, password: await hashAndBase64(user.password ?? '') };
    await firstValueFrom(this.http.post<UserDto>(`${this.userRestPath}`, userWithEncodedPassword));
  }

  public async updateUser(user: UserDto, password: string): Promise<void> {
    const userWithEncodedPassword = {
      ...user,
      password: user.password != null ? await hashAndBase64(user.password) : undefined,
    };
    let params = new HttpParams();
    params = params.append('password', await hashAndBase64(password ?? ''));

    const updatedUser = await firstValueFrom(
      this.http.put<UserDto>(`${this.userRestPath}`, userWithEncodedPassword, {
        params,
        responseType: 'json',
      }),
    );
    this.auth.set(updatedUser);
  }

  public hasRole(roleName: string): boolean {
    const roles = this.currentUser?.roles;
    return roles != null ? roles.some(role => role.name === roleName) : false;
  }

  private async getUserByToken(): Promise<UserDto | null> {
    try {
      return await firstValueFrom(this.http.get<UserDto>(this.authRestPath));
    } catch (e) {
      console.error('Не удалось найти пользователя по токену, пусть пройдет авторизацию', e);
      return null;
    }
  }
}
