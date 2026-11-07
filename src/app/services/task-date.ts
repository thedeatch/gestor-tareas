import { Service } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, timer } from 'rxjs';

@Service()
export class TaskDateService {
  readonly now = toSignal(timer(0, 60_000).pipe(map(() => new Date())), {
    initialValue: new Date()
  });
}
