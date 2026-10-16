import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LoaderComponent } from './components/loader/loader.component';
import { Header } from './components/header/header';
import { ToastModule } from '@openng/optimus-ui/toast';
import { ButtonModule, ButtonSeverity } from '@openng/optimus-ui/button';
import { MessageService, ToastMessageOptions } from '@openng/optimus-ui/api';
import { BlockUI, BlockUIModule } from '@openng/optimus-ui/blockui';
import { BlockedForLiveDirective } from './directives/blocked-for-live.directive';
import { Optimus } from '@openng/optimus-ui/config';
import { ScrollPanelModule } from '@openng/optimus-ui/scrollpanel';

@Component({
  selector: 'pro-legacy-root',
  imports: [
    RouterOutlet,
    LoaderComponent,
    Header,
    ToastModule,
    ButtonModule,
    BlockUIModule,
    BlockedForLiveDirective,
    ScrollPanelModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly MessageServiceKey = MessageServiceKey;
  protected readonly loading = signal<boolean>(true);

  private readonly messageService = inject(MessageService);

  constructor() {
    inject(Optimus).ripple.set(false);
  }

  ngOnInit(): void {
    setTimeout(() => {
      this.loading.set(false);
    }, 750);
  }

  getToastIconClass(message: ToastMessageOptions) {
    if (!message) {
      return 'pi pi-check';
    }
    switch (message.severity) {
      case 'success':
        return 'pi pi-check-circle';
      case 'warn':
        return 'pi pi-exclamation-triangle';
      case 'error':
        return 'pi pi-ban';
      case 'info':
        return 'pi pi-info-circle';
      default:
        return 'pi pi-check';
    }
  }

  getSeverity(message: ToastMessageOptions): ButtonSeverity {
    switch (message?.severity) {
      case 'success':
        return 'success';
      case 'warn':
        return 'warn';
      case 'error':
        return 'danger';
      case 'info':
        return undefined;
      default:
        return 'contrast';
    }
  }

  onToastButtonClick() {
    this.messageService.clear(MessageServiceKey.OK);
  }
}

export enum MessageServiceKey {
  OK = 'MESSAGE_SERVICE_OK_KEY',
}
