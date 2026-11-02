import {CanActivateFn} from '@angular/router';
import {inject} from "@angular/core";
import { UserService } from '../services/auth/user.service';

export const adminGuard: CanActivateFn = (route, state) => {
  return inject(UserService).hasRole('ROLE_admin');
};
