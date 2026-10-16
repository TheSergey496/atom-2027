import { Component, inject, signal } from '@angular/core';
import { ButtonModule } from '@openng/optimus-ui/button';
import { CardModule } from '@openng/optimus-ui/card';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from '@openng/optimus-ui/inputtext';
import { ToastModule } from '@openng/optimus-ui/toast';
import { PasswordModule } from '@openng/optimus-ui/password';
import { UserService } from '../../../services/auth/user.service';
import { UserDto } from '../../../gen/auth-dto';
import { Router } from '@angular/router';
import { MessageService } from '@openng/optimus-ui/api';
import { FloatLabel } from '@openng/optimus-ui/floatlabel';
import { FloatLabelTransitionFixDirective } from '../../../directives/float-label-transition-fix.directive';
import { FormMessageComponent } from '../../forms/form-message/form-message.component';
import { MessageServiceKey } from '../../../app';

@Component({
  imports: [
    ButtonModule,
    CardModule,
    FormsModule,
    InputTextModule,
    ToastModule,
    PasswordModule,
    FloatLabel,
    FloatLabelTransitionFixDirective,
    FormMessageComponent,
  ],
  selector: 'pro-legacy-login-page',
  styleUrl: './login-page.component.css',
  templateUrl: './login-page.component.html',
})
export class LoginPageComponent {
  protected email: string | null = null;
  protected password: string | null = null;
  protected readonly loading = signal(false);

  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);

  constructor() {}

  async login() {
    this.loading.set(true);
    try {
      let user: UserDto | null = await this.userService.login(this.email!, this.password!);
      if (user == null) {
        this.showLoginErrorMessage();
      } else {
        this.messageService.add({
          severity: 'info',
          summary: 'Информация',
          detail: 'Вы успешно авторизованы',
        });
        await this.router.navigate(['']);
      }
    } finally {
      this.loading.set(false);
    }
  }

  async createUser() {
    await this.router.navigate(['registration']);
  }

  private showLoginErrorMessage() {
    this.messageService.add({
      severity: 'error',
      summary: 'Ошибка',
      detail: 'Авторизация не удалась',
    });
  }

  async restorePassword() {
    this.messageService.add({
      key: MessageServiceKey.OK,
      severity: 'info',
      summary: 'Информация',
      detail: 'Обратитесь к администратору для сброса пароля',
      sticky: true,
    });
  }
}
