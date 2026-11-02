import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideOptimus } from '@openng/optimus-ui/config';
import { DefaultTheme } from './themes/defaultTheme';
import { UserService } from './services/auth/user.service';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ConfigService } from './services/config.service';
import { apiInterceptor } from './interceptors/api.interceptor';
import { ConfirmationService, MessageService } from '@openng/optimus-ui/api';
import { authInterceptor } from './interceptors/auth.interceptor';
import { dateInterceptor } from './interceptors/date.interceptor';
import { ru } from '@openng/optimus-ui-locale/js/ru.js';
import { ObjectUtils } from '@openng/optimus-ui/utils';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideOptimus({
      theme: {
        preset: DefaultTheme,
        options: {
          darkModeSelector: false,
        },
      },
      translation: ru,
    }),
    MessageService,
    ConfirmationService,
    provideAppInitializer(async () => {
      const defaultReorderArray = ObjectUtils.reorderArray;
      ObjectUtils.reorderArray =  (value: any[], from: number, to: number) => {
        if (value.length === 0 || from === to) return;

        if (from < 0 || from >= value.length) return;

        to = ((to % value.length) + value.length) % value.length;

        value.splice(to, 0, value.splice(from, 1)[0]);
      }

      const configService = inject(ConfigService);
      const userService = inject(UserService);
      await configService.init();
      await userService.init();
    }),
    provideHttpClient(withInterceptors([apiInterceptor, authInterceptor, dateInterceptor])),
  ],
};
