import { Routes } from '@angular/router';
import { HomePageComponent } from './components/pages/home-page.component/home-page.component';
import { LoginPageComponent } from './components/pages/login-page/login-page.component';
import { RegistrationPageComponent } from './components/pages/registration-page/registration-page.component';
import { unauthorizedGuard } from './guards/unauthorized.guard';
import { authorizedGuard } from './guards/authorized.guard';
import { AdminUsersPageComponent } from './components/pages/admin-users-page/admin-users-page.component';
import { adminGuard } from './guards/admin.guard';
import { EdificesComponent } from './components/pages/edifices/edifices.component';
import { EventsComponent } from './components/pages/events/events.component';
import { EmployeeComponent } from './components/pages/employee/employee.component';
import { BrigadeComponent } from './components/pages/brigade/brigade.component';
import { BrigadeEventsComponent } from './components/pages/brigade-events/brigade-events.component';
import { RepairsComponent } from './components/pages/repairs/repairs.component';
import { CheckComponent } from './components/pages/check/check.component';
import { ProtocolsComponent } from './components/pages/protocols/protocols.component';
// import { ProtocolsComponent } from './components/pages/protocols/protocols.component';

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
  {
    path: 'edifices',
    component: EdificesComponent,
  },
  {
    path: 'employees',
    component: EmployeeComponent,
  },
  {
    path: 'brigades',
    component: BrigadeComponent,
  },
  {
    path: 'events',
    component: EventsComponent,
  },
  {
    path: 'repairs',
    component: RepairsComponent,
  },
  {
    path: 'brigade-events',
    component: BrigadeEventsComponent,
  },
  {
    path: 'protocols',
    component: ProtocolsComponent,
  },
  {
    path: 'checks',
    component: CheckComponent,
  },
  { path: '**', redirectTo: 'home' },
];
