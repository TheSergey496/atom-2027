import { Routes } from '@angular/router';
import { HomePageComponent } from './components/pages/home-page.component/home-page.component';
import { TestPage1Component } from './components/pages/test-page-1.component/test-page-1.component';
import { LoginPageComponent } from './components/pages/login-page/login-page.component';
import { RegistrationPageComponent } from './components/pages/registration-page/registration-page.component';
import { unauthorizedGuard } from './guards/unauthorized.guard';
import { authorizedGuard } from './guards/authorized.guard';

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
    component: TestPage1Component,
    path: 'test-page-1',
    canActivate: [authorizedGuard],
  },
  { path: '**', redirectTo: 'home' },
];
