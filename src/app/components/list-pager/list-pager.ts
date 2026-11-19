import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-list-pager', imports: [MatButtonModule],
  template: `@if (total() > size()) {
    <nav [attr.aria-label]="label()">
      <button mat-button type="button" [disabled]="busy() || page() === 0" (click)="changed.emit(page() - 1)">Anterior</button>
      <span>Página {{ page() + 1 }} · {{ total() }} resultados</span>
      <button mat-button type="button" [disabled]="busy() || (page() + 1) * size() >= total()" (click)="changed.emit(page() + 1)">Siguiente</button>
    </nav>
  }`,
  styles: [`nav { display: flex; gap: .5rem; align-items: center; justify-content: center; flex-wrap: wrap; margin-block: 1rem; }`]
})
export class ListPager {
  readonly total = input(0); readonly page = input(0); readonly size = input(20); readonly busy = input(false);
  readonly label = input('Páginas del listado'); readonly changed = output<number>();
}
