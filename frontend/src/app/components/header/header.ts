import { Component, effect, inject, Renderer2, viewChild } from '@angular/core';
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
import { EdificeService } from '../../gen/atom2027-controllers';
import { GlobalLoadingService } from '../../services/loading.service';
import { GraphSaveStateService } from '../../services/graph/save.service';
import { NgDiagramModelService, provideNgDiagram } from 'ng-diagram';

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
  providers: [provideNgDiagram()],
  selector: 'pro-legacy-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  protected readonly logo = '/assets/images/just_works_logo.png';
  protected readonly logoHover = '/assets/images/just_works_logo_hover.png';
  protected readonly logoHoverAlt = '/assets/images/just_works_logo_hover_alt.png';
  protected logoSrc = this.logo;

  protected menuTabs: MenuTab[] = [];
  protected activeRoute? = '/home';
  protected alertsCount = 0;
  protected readonly userMenuItems: MenuItem[] = [
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

  private readonly edificeService = inject(EdificeService);
  private readonly globalLoadingService = inject(GlobalLoadingService);
  private readonly graphSaveService = inject(GraphSaveStateService);
  private readonly modelService = inject(NgDiagramModelService);

  private readonly renderer = inject(Renderer2);

  public isDarkTheme = false;

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
      if (this.userService.hasRole('ROLE_admin')) {
        this.menuTabs = [
          {
            label: 'Объекты',
            route: '/edificies',
          },
          {
            label: 'События',
            route: '/events',
          },
          {
            label: 'Пользователи',
            route: '/administration/users',
          },
        ];
      } else {
        this.menuTabs = [
          {
            label: 'Объекты',
            route: '/edificies',
          },
          {
            label: 'События',
            route: '/events',
          },
        ];
      }

      const routes = this.menuTabs.map(t => t.route);
      routes.sort((a, b) => b.localeCompare(a));
      this.descSortedRoutes = routes;
      this.activeRoute = this.getActiveRoute(this.router.url);
    });

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd || event instanceof NavigationError))
      .subscribe((event: NavigationEnd | NavigationError) => {
        if (event instanceof NavigationEnd) {
          this.activeRoute = this.getActiveRoute(event.urlAfterRedirects);
          setTimeout(() => this.tabs()?.updateValue(this.activeRoute));
        } else {
          this.activeRoute = '/home';
          setTimeout(() => this.tabs()?.updateValue(this.activeRoute));
        }
      });

    this.activeRoute = this.getActiveRoute(this.router.url);
  }

  private getActiveRoute(url: string): string | undefined {
    return this.descSortedRoutes.find(r => url.startsWith(r));
  }

  protected async navigate(route: string) {
    await this.router.navigate([route]);
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

  public async resetData() {
    await this.globalLoadingService.withLoader(() => {
      this.graphSaveService.clear();
    });
  }

  public switchTheme() {
    this.isDarkTheme = !this.isDarkTheme;
    if (this.isDarkTheme) {
      this.renderer.addClass(document.body, 'darkTheme');
    } else {
      this.renderer.removeClass(document.body, 'darkTheme');
    }
  }
}

interface MenuTab {
  label: string;
  route: string;
}
