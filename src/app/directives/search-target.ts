import { Directive, ElementRef, Injector, afterNextRender, computed, effect, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

@Directive({
  selector: '[appSearchTarget]',
  host: {
    '[style.outline]': "selected() ? '2px solid var(--mat-sys-primary)' : null",
    '[style.outline-offset.px]': 'selected() ? 4 : null'
  }
})
export class SearchTarget {
  readonly appSearchTarget = input.required<number>();
  readonly searchTargetParam = input.required<'publicacion' | 'tarea' | 'evento' | 'album'>();
  private readonly query = toSignal(inject(ActivatedRoute).queryParamMap);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  protected readonly selected = computed(() =>
    Number(this.query()?.get(this.searchTargetParam())) === this.appSearchTarget());

  constructor() {
    effect(onCleanup => {
      if (!this.selected()) return;
      const render = afterNextRender(() => {
        const element = this.element.nativeElement;
        if (!element.hasAttribute('tabindex')) element.tabIndex = -1;
        element.scrollIntoView({ block: 'center' });
        element.focus({ preventScroll: true });
      }, { injector: this.injector });
      onCleanup(() => render.destroy());
    });
  }
}
