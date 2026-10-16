import { Component, computed, input } from '@angular/core';
import { MessageModule } from '@openng/optimus-ui/message';

@Component({
  imports: [MessageModule],
  selector: 'pro-legacy-form-message',
  styleUrl: './form-message.component.css',
  templateUrl: './form-message.component.html',
})
export class FormMessageComponent {
  public message = input<string>('Ошибка');
  public severity = input<'success' | 'info' | 'warn' | 'error'>('error');
  protected icon = computed<string>(() => {
    const severity = this.severity();
    switch (severity) {
      case "success":
        return 'pi pi-check-circle';
      case "info":
        return 'pi pi-info-circle';
      case "warn":
        return 'pi pi-exclamation-triangle';
      case "error":
        return 'pi pi-exclamation-triangle';
    }
  });
}
