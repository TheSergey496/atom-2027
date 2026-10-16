import { CanActivateFn, Router } from '@angular/router';
import {inject} from "@angular/core";
import { UserService } from '../services/auth/user.service';

export const authorizedGuard: CanActivateFn = (route, state) => {
  const user = inject(UserService).currentUser;
  const router = inject(Router);
  return user == null ? router.createUrlTree(['/login']) : true;
};
