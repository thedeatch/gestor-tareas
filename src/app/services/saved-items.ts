import { HttpClient, HttpParams } from '@angular/common/http';
import { DestroyRef, Service, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, defer, finalize, map, shareReplay, tap } from 'rxjs';
import { SavedItemKind, SavedItemsFilter, SavedItemsPage, SavedItemsState } from '../models/saved-item.model';
import { AuthService } from './auth';

@Service()
export class SavedItemsService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  readonly userEmail = this.auth.userEmail;
  private readonly destroyRef = inject(DestroyRef);
  private readonly url = '/api/saved-items';
  private account: string | null = null;
  private generation = 0;
  private readonly values = signal<Record<string, boolean>>({});
  private readonly groups = signal<Record<number, 'loading' | 'ready' | 'error'>>({});
  private readonly pending = signal(new Set<string>());

  findAll(kind: SavedItemsFilter, page: number) {
    this.ensureAccount();
    const params = new HttpParams().set('kind', kind).set('page', page);
    return this.http.get<SavedItemsPage>(this.url, { params });
  }

  loadGroup(groupId: number): void {
    const account = this.ensureAccount();
    const generation = this.generation;
    const status = this.groups()[groupId];
    if (!account || status === 'loading' || status === 'ready') return;
    this.groups.update(groups => ({ ...groups, [groupId]: 'loading' }));
    this.http.get<SavedItemsState>(this.url + '/state', { params: { groupId } })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: state => {
          if (this.auth.userEmail() !== account || this.generation !== generation) return;
          this.values.update(values => {
            const next = { ...values };
            for (const id of state.postIds) next[this.key('POST', id)] = true;
            for (const id of state.imageIds) next[this.key('IMAGE', id)] = true;
            for (const id of state.eventIds ?? []) next[this.key('EVENT', id)] = true;
            return next;
          });
          this.groups.update(groups => ({ ...groups, [groupId]: 'ready' }));
        },
        error: () => {
          if (this.auth.userEmail() === account && this.generation === generation) {
            this.groups.update(groups => ({ ...groups, [groupId]: 'error' }));
          }
        }
      });
  }

  groupStatus(groupId: number) {
    return this.groups()[groupId];
  }

  isSaved(kind: SavedItemKind, targetId: number): boolean {
    return this.account === this.auth.userEmail() && this.values()[this.key(kind, targetId)] === true;
  }

  isPending(kind: SavedItemKind, targetId: number): boolean {
    return this.pending().has(this.key(kind, targetId));
  }

  setSaved(groupId: number, kind: SavedItemKind, targetId: number, saved: boolean) {
    return defer(() => {
      const account = this.ensureAccount();
      const generation = this.generation;
      const key = this.key(kind, targetId);
      if (this.pending().has(key)) return EMPTY;
      this.pending.update(pending => new Set(pending).add(key));
      const request = saved
        ? this.http.put<void>(`${this.url}/${kind}/${targetId}`, { groupId })
        : this.http.delete<void>(`${this.url}/${kind}/${targetId}`);
      return request.pipe(
        tap(() => {
          if (this.auth.userEmail() === account && this.generation === generation) {
            this.values.update(values => ({ ...values, [key]: saved }));
          }
        }),
        map(() => saved),
        finalize(() => {
          if (this.account === account && this.generation === generation) {
            this.pending.update(pending => {
              const next = new Set(pending);
              next.delete(key);
              return next;
            });
          }
        })
      );
    }).pipe(shareReplay({ bufferSize: 1, refCount: false }));
  }

  private ensureAccount(): string | null {
    const account = this.auth.userEmail();
    if (account !== this.account) {
      this.account = account;
      this.generation++;
      this.values.set({});
      this.groups.set({});
      this.pending.set(new Set());
    }
    return account;
  }

  private key(kind: SavedItemKind, targetId: number): string {
    return `${kind}:${targetId}`;
  }
}
