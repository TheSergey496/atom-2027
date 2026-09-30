import { inject, Service } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { UserDto } from '../gen/auth-dto';
import { AuthService } from './auth/auth.service';
import { hashAndBase64 } from '../utils/hash';

@Service()
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  private readonly restPath = 'authentication';

  public async createUser(user: UserDto): Promise<UserDto> {
    user.password = await hashAndBase64(user.password ?? '');
    return await firstValueFrom(this.http.post<UserDto>(`${this.restPath}/registration`, user));
  }

  public async sendRestorePasswordCode(email: string): Promise<void> {
    await firstValueFrom(
      this.http.get(`${this.restPath}/restore-password-code`, {
        params: new HttpParams().append('email', email),
      }),
    );
  }

  public async restorePassword(email: string, newPassword: string, code: number): Promise<void> {
    const password = await hashAndBase64(newPassword);
    await firstValueFrom(
      this.http.patch<UserDto>(
        `${this.restPath}/restore-password`,
        {},
        {
          params: new HttpParams()
            .append('email', email)
            .append('newPassword', password)
            .append('code', code),
        },
      ),
    );
  }
}
