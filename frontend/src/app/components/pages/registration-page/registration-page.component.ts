import { Component, inject, signal } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { Card } from '@openng/optimus-ui/card';
import { FormsModule } from '@angular/forms';
import { MessageService, SharedModule } from '@openng/optimus-ui/api';
import { UserEditFormComponent } from '../../forms/user-edit-form/user-edit-form.component';
import { UserDto } from '../../../gen/auth-dto';
import { Router } from '@angular/router';
import { TooltipModule } from '@openng/optimus-ui/tooltip';
import { UserService } from '../../../services/auth/user.service';

@Component({
  imports: [Button, Card, FormsModule, SharedModule, UserEditFormComponent, TooltipModule],
  selector: 'pro-legacy-registration-page',
  styleUrl: './registration-page.component.css',
  templateUrl: './registration-page.component.html',
})
export class RegistrationPageComponent {
  protected readonly loading = signal(false);
  protected readonly user = new UserDto();
  protected formValid = false;

  private readonly userService = inject(UserService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);

  async createUser() {
    this.loading.set(true);
    try {
      await this.userService.registerUser(this.user);
      this.messageService.add({
        severity: 'success',
        summary: 'Выполнено',
        detail: 'Пользователь успешно зарегистрирован',
      });
      await this.router.navigate(['/login']);
    } finally {
      this.loading.set(false);
    }
  }

  async backToLogin() {
    await this.router.navigate(['/login']);
  }
}
