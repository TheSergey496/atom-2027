import { Component, computed, inject, Signal } from '@angular/core';
import { isActive, Router, RouterOutlet } from '@angular/router';
import { Menubar } from '@openng/optimus-ui/menubar';
import { MenuItem } from '@openng/optimus-ui/api';

@Component({
  selector: 'pro-legacy-root',
  imports: [RouterOutlet, Menubar],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly router = inject(Router);
  private readonly _isHomeComponent;

  protected menuModel: Signal<MenuItem[]> = computed(() => [
    {
      label: this._isHomeComponent() ? 'Супер приложение для победы на Atom Skills' : 'Домой',
      disabled: true,
    },
    { separator: true },
    { icon: 'pi pi-home', disabled: this._isHomeComponent(), routerLink: '/' },
    { separator: true },
    { label: 'Тест 1', routerLink: '/test-page-1' },
  ]);

  constructor() {
    this._isHomeComponent = isActive('', this.router, { paths: 'exact' });
  }
}
