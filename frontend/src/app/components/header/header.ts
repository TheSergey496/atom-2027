import { Component, effect, inject, viewChild } from '@angular/core';
import { NavigationEnd, NavigationError, Router, RouterLink } from '@angular/router';
import { Tabs, TabsModule } from '@openng/optimus-ui/tabs';
import { filter } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { NgOptimizedImage } from '@angular/common';
import { OverlayBadgeModule } from '@openng/optimus-ui/overlaybadge';
import { BadgeModule } from '@openng/optimus-ui/badge';
import { MenuModule } from '@openng/optimus-ui/menu';
import { MenuItem, MessageService } from '@openng/optimus-ui/api';
import { ButtonModule } from '@openng/optimus-ui/button';
import { DialogService } from '../../services/dialog.service';
import { UserEditDialogComponent } from '../forms/user-edit-dialog/user-edit-dialog.component';
import { UserService } from '../../services/auth/user.service';

@Component({
  imports: [
    TabsModule,
    FormsModule,
    Tooltip,
    RouterLink,
    NgOptimizedImage,
    OverlayBadgeModule,
    BadgeModule,
    MenuModule,
    ButtonModule,
  ],
  selector: 'pro-legacy-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  protected menuTabs: MenuTab[] = [];
  protected activeRoute? = '/';
  protected alertsCount = 0;
  protected userMenuItems: MenuItem[] = [
    {
      label: 'Управление аккаунтом',
      icon: 'pi pi-cog',
      command: () => this.editUser(),
    },
    {
      label: 'Выйти',
      icon: 'pi pi-sign-out',
      linkClass: 'sign-out-option',
      command: async () => {
        this.activeRoute = undefined;
        await this.userService.logout();
      },
    },
  ];

  protected readonly userService = inject(UserService);

  private readonly router = inject(Router);
  private readonly dialogService = inject(DialogService);
  private readonly messageService = inject(MessageService);
  private readonly tabs = viewChild<Tabs>('tabs');

  private descSortedRoutes: string[] = [];

  protected get userRolesString() {
    if (this.userService.currentUser?.rolesAsString != null) {
      return `Текущая роль: ${this.userService.currentUser.rolesAsString}`;
    } else {
      return 'Нет доступных ролей';
    }
  }

  protected get userName() {
    const user = this.userService.currentUser;
    return user
      ? `${user.lastname ?? ''} ${user.firstname ? user.firstname[0].toUpperCase() + '.' : ''} ${user.surname ? user.surname[0].toUpperCase() + '.' : ''}`
      : 'Пользователь не авторизован';
  }

  constructor() {
    // тут типо чтобы от ролей разное показывать
    effect(() => {
      const user = this.userService.currentUser;
      if (user != null) {
        this.menuTabs = [
          {
            label: 'Home',
            route: '/',
          },
          {
            label: 'Test',
            route: '/test-page-1',
          },
        ];
      } else {
        this.menuTabs = [];
      }

      const routes = this.menuTabs.map(t => t.route);
      routes.sort((a, b) => b.localeCompare(a));
      this.descSortedRoutes = routes;
    });

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd || event instanceof NavigationError))
      .subscribe((event: NavigationEnd | NavigationError) => {
        if (event instanceof NavigationEnd) {
          this.activeRoute = this.getActiveRoute(event.urlAfterRedirects);
        } else {
          this.activeRoute = '/';
          setTimeout(() => this.tabs()?.updateValue(this.activeRoute));
        }
      });

    // this.activeRoute = this.getActiveRoute(this.router.url);
    this.activeRoute = undefined;
  }

  private getActiveRoute(url: string): string {
    const route = this.descSortedRoutes.find(r => url.startsWith(r));
    return route ?? '/';
  }

  protected async navigate(route: string) {
    try {
      await this.router.navigate([route]);
    } catch {
      await this.router.navigate(['/']);
    }
  }

  private editUser() {
    const dialog = this.dialogService.createDialog(UserEditDialogComponent);
    dialog.callbacks = {
      save: async (dialog, result) => {
        dialog.loading = true;
        try {
          await this.userService.updateUser(result!.user!, result!.currentPassword!);
          this.messageService.add({
            severity: 'success',
            summary: 'Выполнено',
            detail: 'Пользовательские данные обновлены',
          });

          dialog.visible = false;
        } finally {
          dialog.loading = false;
        }
      },
    };
  }
}

interface MenuTab {
  label: string;
  route: string;
}
