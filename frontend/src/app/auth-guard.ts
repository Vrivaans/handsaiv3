import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';
import { map } from 'rxjs/operators';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.getAuthStatus().pipe(
    map(status => {
      // 1. If setup is not complete, forcing to go to setup page
      if (!status.setupComplete) {
        if (state.url !== '/auth/setup') {
          return router.createUrlTree(['/auth/setup']);
        }
        return true; // Already on setup
      }

      // 2. If it is complete but the user tries to access /auth/setup, redirect to login
      if (state.url === '/auth/setup') {
        return router.createUrlTree(['/auth/login']);
      }

      // Require login for other routes? For this requirement, login is ONLY needed
      // for the token management operations, NOT the rest of the application. 
      // The rest of the app doesn't require a login since it's local.
      return true;
    })
  );
};
