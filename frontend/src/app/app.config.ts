import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideOptimus } from '@openng/optimus-ui/config';
import { DefaultTheme } from './themes/defaultTheme';

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
    }),
  ],
};
