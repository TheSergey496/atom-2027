import { HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { inject } from '@angular/core';
import { TokenStorageService } from '../services/auth/tokenStorage.service';

export function authInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> {
  const tokenStorageService = inject(TokenStorageService);
  const accessToken = tokenStorageService.getAccessToken();
  if (accessToken != null) {
    req = req.clone({
      headers: req.headers.set('Authorization', 'Bearer ' + accessToken),
    });
  }
  return next(req);
}
