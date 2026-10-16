import {
  Directive,
  effect,
  forwardRef,
  input,
  Input,
  OnChanges,
  SimpleChanges,
  untracked,
} from '@angular/core';
import { AbstractControl, NG_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';

@Directive({
  selector: '[proLegacyPasswordsMatch]',
  providers: [
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PasswordsMatchDirective),
      multi: true,
    },
  ],
})
export class PasswordsMatchDirective implements Validator {
  public readonly proLegacyPasswordsMatch = input<string | undefined>('');

  private onValidatorChange?: () => void;

  constructor() {
    effect(() => {
      this.proLegacyPasswordsMatch();
      untracked(() => this.onValidatorChange?.());
    });
  }

  validate(control: AbstractControl): ValidationErrors | null {
    return control.value === this.proLegacyPasswordsMatch() ? null : { passwordMismatch: true };
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }
}
