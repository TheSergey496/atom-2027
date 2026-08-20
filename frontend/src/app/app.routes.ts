import { Routes } from '@angular/router';
import { HomePageComponent } from './components/pages/home-page.component/home-page.component';
import { TestPage1Component } from './components/pages/test-page-1.component/test-page-1.component';

export const routes: Routes = [
  {
    component: HomePageComponent,
    path: '',
  },
  {
    component: TestPage1Component,
    path: 'test-page-1',
  },
];
