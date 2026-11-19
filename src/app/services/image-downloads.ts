import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable, Subscription } from 'rxjs';

@Service()
export class ImageDownloads {
  private readonly http = inject(HttpClient);
  private active = 0;
  private readonly queue: (() => void)[] = [];

  get(url: string): Observable<Blob> {
    return new Observable(observer => {
      let request: Subscription | undefined;
      let started = false;
      let released = false;
      const start = () => {
        started = true;
        this.active++;
        request = this.http.get(url, { responseType: 'blob' }).subscribe(observer);
      };
      this.queue.push(start);
      this.drain();
      return () => {
        if (released) return;
        released = true;
        const index = this.queue.indexOf(start);
        if (index >= 0) this.queue.splice(index, 1);
        request?.unsubscribe();
        if (started) this.active--;
        this.drain();
      };
    });
  }

  private drain(): void {
    while (this.active < 4 && this.queue.length) this.queue.shift()!();
  }
}
