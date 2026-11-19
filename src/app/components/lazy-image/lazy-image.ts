import { Component, ElementRef, effect, inject, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Subscription } from 'rxjs';
import { ImageDownloads } from '../../services/image-downloads';

@Component({
  selector: 'app-lazy-image',
  imports: [MatIconModule],
  template: `@if (imageUrl(); as source) { <img [src]="source" [alt]="alt()" decoding="async" /> }
    @else { <mat-icon aria-hidden="true">{{ failed() ? 'broken_image' : 'photo' }}</mat-icon> }`,
  styles: [`:host { display: grid; place-items: center; width: 100%; height: 100%; overflow: hidden; }
    img { width: 100%; height: 100%; object-fit: cover; }`]
})
export class LazyImage {
  readonly src = input.required<string>();
  readonly alt = input('');
  protected readonly imageUrl = signal<string | null>(null);
  protected readonly failed = signal(false);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly downloads = inject(ImageDownloads);

  constructor() {
    effect(onCleanup => {
      const url = this.src();
      this.imageUrl.set(null);
      this.failed.set(false);
      let request: Subscription | undefined;
      let objectUrl: string | undefined;
      let observer: IntersectionObserver | undefined;
      let requested = false;
      const load = () => {
        if (requested) return;
        requested = true;
        observer?.disconnect();
        request = this.downloads.get(url).subscribe({
          next: blob => { objectUrl = URL.createObjectURL(blob); this.imageUrl.set(objectUrl); },
          error: () => this.failed.set(true)
        });
      };
      if (typeof IntersectionObserver === 'undefined') load();
      else {
        observer = new IntersectionObserver(entries => {
          if (entries.some(entry => entry.isIntersecting)) load();
        }, { rootMargin: '240px' });
        observer.observe(this.host.nativeElement);
      }
      onCleanup(() => {
        observer?.disconnect();
        request?.unsubscribe();
        if (objectUrl) URL.revokeObjectURL(objectUrl);
      });
    });
  }
}
