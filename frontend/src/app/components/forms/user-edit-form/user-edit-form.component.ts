import {
  AfterViewInit,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { RoleDto, UserDto } from '../../../gen/auth-dto';
import { FormsModule, NgForm } from '@angular/forms';
import { distinctUntilChanged, startWith } from 'rxjs';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InputTextModule } from '@openng/optimus-ui/inputtext';
import { PasswordModule } from '@openng/optimus-ui/password';
import { FloatLabelModule } from '@openng/optimus-ui/floatlabel';
import { FloatLabelTransitionFixDirective } from '../../../directives/float-label-transition-fix.directive';
import { PasswordsMatchDirective } from '../../../validators/passwords-match.directive';
import { MessageModule } from '@openng/optimus-ui/message';
import { NgTemplateOutlet } from '@angular/common';
import { ToggleSwitchModule } from '@openng/optimus-ui/toggleswitch';
import { SelectButtonModule } from '@openng/optimus-ui/selectbutton';
import { FormMessageComponent } from '../../library/form-message/form-message.component';
import { SelectModule } from '@openng/optimus-ui/select';

@Component({
  imports: [
    FormsModule,
    InputTextModule,
    PasswordModule,
    FloatLabelModule,
    FloatLabelTransitionFixDirective,
    PasswordsMatchDirective,
    MessageModule,
    NgTemplateOutlet,
    ToggleSwitchModule,
    SelectButtonModule,
    FormMessageComponent,
    SelectModule,
  ],
  selector: 'pro-legacy-user-edit-form',
  styleUrl: './user-edit-form.component.css',
  templateUrl: './user-edit-form.component.html',
})
export class UserEditFormComponent implements AfterViewInit {
  public validityChange = output<boolean>();
  public currentPasswordChange = output<string>();
  public mode = input.required<'registration' | 'edit' | 'adminCreate' | 'adminEdit'>();
  public user = input.required<UserDto>();
  public disabled = input<boolean>(false);
  public roles = input<RoleDto[]>([]);

  protected readonly emailRequired = computed<boolean>(() => {
    switch (this.mode()) {
      case 'edit':
      case 'adminEdit':
        return false;
      default:
        return true;
    }
  });
  protected passwordAgain: string | undefined = undefined;
  protected currentPassword: string | undefined = undefined;
  protected changePasswordOptions = [
    { label: 'Да', value: true },
    { label: 'Нет', value: false },
  ];
  protected changePassword = false;

  private form = viewChild.required<NgForm>('form');
  private readonly destroyRef = inject(DestroyRef);

  protected get userRole(): RoleDto {
    return (this.user().roles ?? [])[0];
  }

  ngAfterViewInit() {
    this.form()
      .statusChanges!.pipe(
        startWith(this.form().status),
        map(() => this.form().valid),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(valid => this.validityChange.emit(!!valid));
  }

  protected onCurrentPasswordChange(newValue: string) {
    this.currentPassword = newValue;
    this.currentPasswordChange.emit(newValue);
  }

  protected onChangePasswordChange(newValue: boolean) {
    this.changePassword = newValue;
    this.user().password = undefined;
    this.passwordAgain = undefined;
  }
}
