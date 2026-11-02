import { Component, inject, signal } from '@angular/core';
import { Button } from '@openng/optimus-ui/button';
import { Dialog } from '@openng/optimus-ui/dialog';
import { UserEditFormComponent } from '../user-edit-form/user-edit-form.component';
import { RoleDto, UserDto } from '../../../gen/auth-dto';
import { DialogComponent } from '../../../utils/dialogComponent';
import { UserEditDialogResult } from '../user-edit-dialog/user-edit-dialog.component';
import { UserAdminService } from '../../../gen/controllers-user';
import { lastValueFrom } from 'rxjs';

@Component({
  imports: [Button, Dialog, UserEditFormComponent],
  selector: 'pro-legacy-admin-user-edit-dialog',
  styleUrl: './admin-user-edit-dialog.component.css',
  templateUrl: './admin-user-edit-dialog.component.html',
})
export class AdminUserEditDialogComponent extends DialogComponent<UserDto, 'save'> {
  protected readonly mode = signal<'adminCreate' | 'adminEdit'>('adminCreate');
  protected readonly header = signal<string | undefined>(undefined);
  protected readonly roles = signal<RoleDto[]>([]);

  protected user: UserDto = new UserDto();
  protected formValid = false;

  private readonly userAdminService = inject(UserAdminService);

  constructor() {
    super();
  }

  public async init(userId?: number) {
    this.loading = true;
    try {
      if (userId != null) {
        this.header.set('Добавление пользователя');
        this.mode.set('adminEdit');
        this.user = await lastValueFrom(this.userAdminService.getUser(userId, true));
      } else {
        this.header.set('Редактирование пользователя');
      }
      this.roles.set(await lastValueFrom(this.userAdminService.getRoles()));
    } catch (e) {
      this.visible = false;
      throw e;
    } finally {
      this.loading = false;
    }
  }

  protected async save() {
    await this._callbacks.save(this, this.user);
  }
}
