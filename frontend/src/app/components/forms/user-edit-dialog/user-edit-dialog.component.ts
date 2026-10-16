import { Component, inject } from '@angular/core';
import { DialogComponent } from '../../../utils/dialogComponent';
import { UserDto } from '../../../gen/auth-dto';
import { Dialog } from '@openng/optimus-ui/dialog';
import { UserEditFormComponent } from '../user-edit-form/user-edit-form.component';
import { UserService } from '../../../services/auth/user.service';
import { Button } from '@openng/optimus-ui/button';

@Component({
  imports: [Dialog, UserEditFormComponent, Button],
  selector: 'pro-legacy-user-edit-dialog',
  styleUrl: './user-edit-dialog.component.css',
  templateUrl: './user-edit-dialog.component.html',
})
export class UserEditDialogComponent extends DialogComponent<UserEditDialogResult, 'save'> {
  protected user: UserDto;
  protected currentPassword = '';
  protected formValid = false;

  private userService = inject(UserService);

  constructor() {
    super();
    let currentUser = this.userService.currentUser;
    this.user = currentUser != null ? JSON.parse(JSON.stringify({...currentUser})) : new UserDto();
  }

  protected async save() {
    await this._callbacks.save(this, new UserEditDialogResult(this.user, this.currentPassword));
  }
}

export class UserEditDialogResult {
  public readonly user: UserDto;
  public readonly currentPassword: string;

  constructor(user: UserDto, currentPassword: string) {
    this.user = user;
    this.currentPassword = currentPassword;
  }
}
