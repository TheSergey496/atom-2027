import { inject, Service, signal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { TokenStorageService } from './tokenStorage.service';
import { UserDto } from '../../gen/auth-dto';
import { hashAndBase64 } from '../../utils/hash';

@Service()
export class AuthService {
  private readonly restPath = 'authentication';
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  private readonly auth = signal<UserDto | null>(null);

  public async init() {
    this.auth.set(await this.getUserByToken());
  }

  public get currentUser(): UserDto | null {
    return this.auth();
  }

  public async login(email: string, password: string): Promise<UserDto | null> {
    const response: HttpResponse<UserDto> = await firstValueFrom(
      this.http.post<UserDto>(
        this.restPath,
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
    await firstValueFrom(this.http.delete(this.restPath));
    this.auth.set(null);
    this.tokenStorage.deleteAccessToken();
  }

  public hasRole(roleName: string): boolean {
    const roles = this.currentUser?.roles;
    return roles != null ? roles.some((role) => role.name === roleName) : false;
  }

  private async getUserByToken(): Promise<UserDto | null> {
    try {
      return await firstValueFrom(this.http.get<UserDto>(this.restPath));
    } catch (e) {
      console.error('Не удалось найти пользователя по токену, пусть пройдет авторизацию', e);
      return null;
    }
  }
}
