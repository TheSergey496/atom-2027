import {CanActivateFn} from '@angular/router';
import {inject} from "@angular/core";
import { UserService } from '../services/auth/user.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const user = inject(UserService).currentUser;
  if (user) {
    return !!user.roles?.find(r => r.name === 'ROLE_admin')
  }
  return false;
};
