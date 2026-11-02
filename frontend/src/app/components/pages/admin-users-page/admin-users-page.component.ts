import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { CardModule } from '@openng/optimus-ui/card';
import { TableComponent } from '../../library/table/table.component';
import { ButtonModule } from '@openng/optimus-ui/button';
import { InputNumberModule } from '@openng/optimus-ui/inputnumber';
import { FormsModule } from '@angular/forms';
import { Column } from '../../library/table/column';
import { ToolbarModule } from '@openng/optimus-ui/toolbar';
import { UserDto } from '../../../gen/auth-dto';
import { UserAdminService } from '../../../gen/controllers-user';
import { lastValueFrom } from 'rxjs';
import { TooltipModule } from '@openng/optimus-ui/tooltip';
import { DialogService } from '../../../services/dialog.service';
import { AdminUserEditDialogComponent } from '../../forms/admin-user-edit-dialog/admin-user-edit-dialog.component';
import { MessageService } from '@openng/optimus-ui/api';
import { SyncConfirmationService } from '../../../services/sync-confirmation.service';
import { BlockUIModule } from '@openng/optimus-ui/blockui';

@Component({
  imports: [
    CardModule,
    TableComponent,
    ButtonModule,
    InputNumberModule,
    FormsModule,
    ToolbarModule,
    TooltipModule,
    BlockUIModule,
  ],
  selector: 'pro-legacy-admin-users-page',
  styleUrl: './admin-users-page.component.css',
  templateUrl: './admin-users-page.component.html',
})
export class AdminUsersPageComponent implements OnInit {
  protected readonly columns = signal<Column[]>([
    {
      type: 'int',
      field: 'id',
      header: 'ID',
      width: 120,
    },
    {
      type: 'text',
      field: 'fullName',
      header: 'ФИО',
      width: 350,
    },
    {
      type: 'text',
      field: 'email',
      header: 'Почта',
      width: 280,
    },
    {
      type: 'date',
      field: 'registrationDate',
      header: 'Дата регистрации',
      width: 255,
    },
    {
      type: 'text',
      field: 'rolesAsString',
      header: 'Роль',
    },
    {
      type: 'boolean',
      field: 'archived',
      header: 'Архив',
      width: 140,
    },
  ]);
  protected readonly getRowId = (row: UserDto) => `${row?.id}`;
  protected readonly filter = signal<boolean>(false);
  protected readonly loading = signal<boolean>(false);
  protected readonly users = signal<UserDto[]>([]);
  protected selectedUser = signal<UserDto | null>(null);
  protected readonly blockingMask = signal<boolean>(false);

  private readonly table = viewChild<TableComponent>('table');
  private readonly userAdminService = inject(UserAdminService);
  private readonly dialogService = inject(DialogService);
  private readonly messageService = inject(MessageService);
  private readonly syncConfirmationService = inject(SyncConfirmationService);

  constructor() {}

  ngOnInit() {
    this.loadData().then();
  }

  protected async loadData() {
    this.loading.set(true);
    this.users.set([]);
    try {
      const users = await lastValueFrom(this.userAdminService.getUsers());
      this.users.set(users);
      console.log('load');
    } finally {
      this.loading.set(false);
    }
  }

  protected async createUser() {
    const dialog = this.dialogService.createDialog(AdminUserEditDialogComponent);
    await dialog.init();
    dialog.callbacks = {
      save: async (dialog, user) => {
        dialog.loading = true;
        try {
          const newUser = await lastValueFrom(this.userAdminService.createUser(user!));
          this.messageService.add({
            severity: 'success',
            summary: 'Выполнено',
            detail: 'Пользователь добавлен',
          });
          await this.loadData();
          await this.table()?.findAndSelectRow(newUser);

          dialog.visible = false;
        } finally {
          dialog.loading = false;
        }
      },
    };
  }

  protected async editUser() {
    const userId = this.selectedUser()?.id;
    if (userId == null) {
      return;
    }

    const dialog = this.dialogService.createDialog(AdminUserEditDialogComponent);
    await dialog.init(userId);
    dialog.callbacks = {
      save: async (dialog, user) => {
        dialog.loading = true;
        try {
          const updatedUser = await lastValueFrom(this.userAdminService.updateUser(userId, user!));
          this.messageService.add({
            severity: 'success',
            summary: 'Выполнено',
            detail: 'Данные пользователя сохранены',
          });
          this.table()?.updateRows([updatedUser]);
          this.selectedUser.set(updatedUser);
          dialog.visible = false;
        } finally {
          dialog.loading = false;
        }
      },
    };
  }

  protected async resetUserPassword() {
    const userId = this.selectedUser()?.id;
    if (userId == null) {
      return;
    }

    const b = await this.syncConfirmationService.confirm({
      message: 'Вы уверены, что хотите сбросить пароль выбранного пользователя?',
    });

    if (!b) {
      return;
    }

    this.blockingMask.set(true);
    try {
      await lastValueFrom(this.userAdminService.resetPassword(userId));
      this.messageService.add({
        severity: 'success',
        summary: 'Выполнено',
        detail: 'Пароль пользователя сброшен',
      });
    } finally {
      this.blockingMask.set(false);
    }
  }

  protected async archiveUser() {
    const userId = this.selectedUser()?.id;
    if (userId == null) {
      return;
    }

    const b = await this.syncConfirmationService.confirm({
      message: 'Вы уверены, что хотите архивировать выбранного пользователя?',
    });

    if (!b) {
      return;
    }

    this.blockingMask.set(true);
    try {
      const updatedUser = await lastValueFrom(this.userAdminService.archiveUser(userId));
      this.messageService.add({
        severity: 'success',
        summary: 'Выполнено',
        detail: 'Пользователь архивирован',
      });

      this.table()?.updateRows([updatedUser]);
      this.selectedUser.set(updatedUser);
    } finally {
      this.blockingMask.set(false);
    }
  }

  protected async unarchiveUser() {
    const userId = this.selectedUser()?.id;
    if (userId == null) {
      return;
    }

    const b = await this.syncConfirmationService.confirm({
      message: 'Вы уверены, что хотите восстановить выбранного пользователя из архива?',
    });

    if (!b) {
      return;
    }

    this.blockingMask.set(true);
    try {
      const updatedUser = await lastValueFrom(this.userAdminService.unarchiveUser(userId));
      this.messageService.add({
        severity: 'success',
        summary: 'Выполнено',
        detail: 'Пользователь восстановлен из архива',
      });

      this.table()?.updateRows([updatedUser]);
      this.selectedUser.set(updatedUser);
    } finally {
      this.blockingMask.set(false);
    }
  }
}
