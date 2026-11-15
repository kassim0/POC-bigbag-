import { HttpErrorResponse, HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, from, switchMap, throwError } from 'rxjs';
import { SessionService } from './session';

/** Adds the wallet token to API calls; if the server no longer knows it, starts over as a new guest. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }
  const session = inject(SessionService);
  const send = (retryOnUnauthorized: boolean): Observable<HttpEvent<unknown>> =>
    from(session.token()).pipe(
      switchMap((token) =>
        next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
          catchError((error: unknown) => {
            if (retryOnUnauthorized && error instanceof HttpErrorResponse && error.status === 401) {
              return from(session.invalidate(token)).pipe(switchMap(() => send(false)));
            }
            return throwError(() => error);
          }),
        ),
      ),
    );
  return send(true);
};
