import { Component, computed, signal } from '@angular/core';
import { Dialog } from '@openng/optimus-ui/dialog';
import { DialogComponent } from '../../../utils/dialogComponent';
import { UserDto } from '../../../gen/auth-dto';
import { FormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { InputText } from '@openng/optimus-ui/inputtext';
import { Password } from '@openng/optimus-ui/password';
import { Button } from '@openng/optimus-ui/button';

@Component({
  imports: [Dialog, FormsModule, NgTemplateOutlet, InputText, Password, Button],
  selector: 'pro-legacy-registration-dialog.component',
  styleUrl: './registration-dialog.component.css',
  templateUrl: './registration-dialog.component.html',
})
export class RegistrationDialogComponent extends DialogComponent<UserDto, 'login' | 'register'> {
  protected mode = signal<'register' | 'login'>('login');
  protected readonly header = computed(() =>
    this.mode() === 'login' ? 'Вход в систему' : 'Регистрация',
  );

  constructor() {
    super();
    this.res = new UserDto();
  }

  protected login() {
    void this._callbacks.login(this, this.res);
  }
}
