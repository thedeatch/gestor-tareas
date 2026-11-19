import { Component, effect, inject, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-user-avatar',
  imports: [MatIconModule],
  host: { role: 'img', '[attr.aria-label]': 'label()', '[style.width.px]': 'size()', '[style.height.px]': 'size()' },
  template: `@if (url(); as source) { <img [src]="source" alt="" /> } @else { <mat-icon aria-hidden="true">person</mat-icon> }`,
  styles: [`
    :host { display: inline-grid; place-items: center; flex-shrink: 0; border-radius: 50%; overflow: hidden;
      background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container); }
    img { display: block; width: 100%; height: 100%; object-fit: cover; }
  `]
})
export class UserAvatar {
  readonly userId = input.required<number>();
  readonly hasAvatar = input(false);
  readonly label = input('Foto de perfil');
  readonly size = input(32);
  protected readonly url = signal<string | null>(null);
  private readonly auth = inject(AuthService);

  constructor() {
    effect(onCleanup => {
      const id = this.userId();
      const available = this.hasAvatar();
      this.url.set(null);
      if (!available) return;
      let objectUrl: string | null = null;
      const request = this.auth.getPublicAvatar(id).subscribe({
        next: blob => { objectUrl = URL.createObjectURL(blob); this.url.set(objectUrl); },
        error: () => this.url.set(null)
      });
      onCleanup(() => {
        request.unsubscribe();
        if (objectUrl) URL.revokeObjectURL(objectUrl);
      });
    });
  }
}
