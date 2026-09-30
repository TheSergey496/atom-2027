import { Component, inject } from '@angular/core';
import { isActive, Router, RouterLink, RouterOutlet } from '@angular/router';
import { Button } from '@openng/optimus-ui/button';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { DialogService } from './services/dialog.service';
import { RegistrationDialogComponent } from './components/forms/registration-dialog.component/registration-dialog.component';
import { AuthService } from './services/auth/auth.service';

@Component({
  selector: 'pro-legacy-root',
  imports: [RouterOutlet, Button, Toolbar, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly isHomeComponent;
  private readonly router = inject(Router);
  private readonly dialogService = inject(DialogService);
  private readonly authService = inject(AuthService);

  protected get currentUser() {
    return this.authService.currentUser;
  }

  constructor() {
    this.isHomeComponent = isActive('', this.router, { paths: 'exact' });
  }

  protected openLoginForm() {
    const dialog = this.dialogService.createDialog(RegistrationDialogComponent);
    dialog.callbacks = {
      login: async (dialog, res) => {
        if (res == null) {
          dialog.visible = false;
          return;
        }
        try {
          dialog.loading = true;
          await this.authService.login(res.email ?? '', res.password ?? '');
          dialog.visible = false;
        } finally {
          dialog.loading = false;
        }
      },
      register: () => {
        /* empty */
      },
    };
  }
}

export enum MessageServiceKey {
  OK = 'MESSAGE_SERVICE_OK_KEY',
}
