import {
  AfterViewInit,
  Directive,
  ElementRef,
  inject,
  Renderer2,
  RendererStyleFlags2,
} from '@angular/core';

@Directive({
  selector: 'p-floatlabel[proLegacyFloatLabelTransitionFix]',
})
export class FloatLabelTransitionFixDirective implements AfterViewInit {
  private readonly element = inject(ElementRef<HTMLElement>);
  private readonly renderer = inject(Renderer2);

  constructor() {
    this.renderer.addClass(this.element.nativeElement, 'no-label-animation');
  }

  ngAfterViewInit(): void {
    requestAnimationFrame(() =>
      this.renderer.removeClass(this.element.nativeElement, 'no-label-animation'),
    );
  }
}
