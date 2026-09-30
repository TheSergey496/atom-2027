import { HttpErrorResponse, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { ConfigService } from '../services/config.service';
import { MessageService } from '@openng/optimus-ui/api';
import { catchError } from 'rxjs';
import { MessageServiceKey } from '../app';
import { environment } from '../../environments/environment';

const handledErrors = ['BusinessLogicException', 'InsufficientPrivilegesException'];

export function apiInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  const configService = inject(ConfigService);
  const messageService = inject(MessageService);

  const showError = (message?: string) => {
    messageService.add({
      key: MessageServiceKey.OK,
      sticky: true,
      severity: 'error',
      summary: 'Ошибка',
      detail: message ?? 'Что-то пошло не так',
    });
  };

  if (!req.url.toLocaleUpperCase().startsWith('HTTP') && !req.url.includes('.json')) {
    req = req.clone({ url: `${configService.baseUrl}/${req.url}` });
  }
  return next(req).pipe(
    catchError(async (response: HttpErrorResponse) => {
      try {
        switch (response.status) {
          case 401: {
            showError('Ошибка ресурсного доступа');
            break;
          }
          case 0: {
            showError('Бэкенд не запущен');
            break;
          }
          default: {
            const error = await parseServerError(response.error);

            if (error != null && handledErrors.includes(error.className)) {
              showError(error.message);
            } else if (environment.production) {
              console.error(`Непредусмотренная ошибка: ${response.message}`);
            } else {
              showError(response.message);
            }
          }
        }
      } catch (e) {
        if (environment.production) {
          console.error(`Не удалось распарсить ошибку: ${response.message}`, e);
        } else {
          showError(response.message);
        }
      }
      throw response.error;
    }),
  );
}

interface ServerError {
  className: string;
  message: string;
}

const isServerError = (value: unknown): value is ServerError => {
  if (typeof value !== 'object' || value === null) return false;

  const obj = value as Record<string, unknown>;
  return typeof obj['className'] === 'string' && typeof obj['message'] === 'string';
};

const parseServerError = async (raw: unknown): Promise<ServerError | null> => {
  let value: unknown = raw;

  if (typeof value === 'string') {
    try {
      value = JSON.parse(value);
    } catch {
      return null;
    }
  }

  if (value instanceof Blob) {
    try {
      value = JSON.parse(await value.text());
    } catch {
      return null;
    }
  }

  return isServerError(value) ? value : null;
};
