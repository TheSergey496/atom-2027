import { Routes } from '@angular/router';
import { HomePageComponent } from './components/pages/home-page.component/home-page.component';
import { LoginPageComponent } from './components/pages/login-page/login-page.component';
import { RegistrationPageComponent } from './components/pages/registration-page/registration-page.component';
import { unauthorizedGuard } from './guards/unauthorized.guard';
import { authorizedGuard } from './guards/authorized.guard';
import { AdminUsersPageComponent } from './components/pages/admin-users-page/admin-users-page.component';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    component: LoginPageComponent,
    path: 'login',
    canActivate: [unauthorizedGuard],
  },
  {
    component: RegistrationPageComponent,
    path: 'registration',
    canActivate: [unauthorizedGuard],
  },
  {
    component: HomePageComponent,
    path: 'home',
    canActivate: [authorizedGuard],
  },
  {
    path: 'administration',
    children: [
      {
        path: 'users',
        component: AdminUsersPageComponent,
        canActivate: [adminGuard],
      },
    ],
  },
  { path: '**', redirectTo: 'home' },
];
