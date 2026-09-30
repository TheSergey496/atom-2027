import { HttpEvent, HttpHandlerFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { map } from 'rxjs';

export function dateInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  return next(req).pipe(
    map((event: HttpEvent<unknown>) => {
      if (event instanceof HttpResponse && req.responseType == 'json') {
        convertToDate(event.body, '', new Map<string, CacheType>());
        event = event.clone({ body: event.body });
      }
      return event;
    }),
  );
}

const dateRegEx =
  /^((\d{4}-\d\d-\d\d)|(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?(([+-]\d\d:\d\d)|Z)?))$/;
type CacheType = 'date' | 'object' | 'other';

function convertToDate(body: unknown, path: string, typesCache: Map<string, CacheType>) {
  if (body == null || typeof body !== 'object') return;

  const obj = body as Record<string, unknown>;
  const isArray = Array.isArray(obj);

  for (const key of Object.keys(obj)) {
    const value = obj[key];
    const curPath = isArray ? `${path}[${key}]` : `${path}.${key}`;
    const cachedType = typesCache.get(curPath);

    if (cachedType === 'date') {
      if (typeof value === 'string') obj[key] = new Date(value);
    } else if (cachedType === 'object') {
      convertToDate(value, curPath, typesCache);
    } else {
      const isExcluded = key === 'value' && 'type' in obj;

      if (!isExcluded && typeof value === 'string' && dateRegEx.test(value)) {
        obj[key] = new Date(value);
        typesCache.set(curPath, 'date');
      } else if (value && typeof value === 'object') {
        convertToDate(value, curPath, typesCache);
        typesCache.set(curPath, 'object');
      } else {
        typesCache.set(curPath, 'other');
      }
    }
  }
}
