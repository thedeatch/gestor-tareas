import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Subscription, finalize } from 'rxjs';
import { CommunitySearchFilter, CommunitySearchKind, CommunitySearchPage, CommunitySearchResult } from '../../models/community-search.model';
import { getApiErrorMessage } from '../../models/api-error.model';
import { CommunitySearchService } from '../../services/community-search';

@Component({
  selector: 'app-community-search',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatSelectModule],
  templateUrl: './community-search.html',
  styleUrl: './community-search.css'
})
export class CommunitySearch implements OnInit {
  readonly groupId = input.required<number>();
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(CommunitySearchService);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  protected readonly result = signal<CommunitySearchPage | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly filters: { kind: CommunitySearchFilter; label: string }[] = [
    { kind: 'ALL', label: 'Todo el contenido' }, { kind: 'POST', label: 'Publicaciones' },
    { kind: 'TASK', label: 'Tareas' }, { kind: 'EVENT', label: 'Eventos' }, { kind: 'ALBUM', label: 'Álbumes' }
  ];
  protected readonly types: Record<CommunitySearchKind, { label: string; icon: string; section: string; param: string }> = {
    POST: { label: 'Publicación', icon: 'article', section: 'posts', param: 'publicacion' },
    TASK: { label: 'Tarea', icon: 'task_alt', section: 'productivity', param: 'tarea' },
    EVENT: { label: 'Evento', icon: 'event', section: 'events', param: 'evento' },
    ALBUM: { label: 'Álbum', icon: 'photo_library', section: 'events', param: 'album' }
  };
  protected readonly form = new FormGroup({
    query: new FormControl(this.route.snapshot.queryParamMap.get('busqueda') ?? '', {
      nonNullable: true, validators: [Validators.maxLength(200)]
    }),
    kind: new FormControl<CommunitySearchFilter>('ALL', { nonNullable: true })
  });
  private lastQuery = '';
  private lastKind: CommunitySearchFilter = 'ALL';

  ngOnInit(): void {
    const kind = this.route.snapshot.queryParamMap.get('tipoBusqueda');
    const filter = this.filters.find(filter => filter.kind === kind);
    if (filter) this.form.controls.kind.setValue(filter.kind);
    if (this.form.controls.query.value.trim()) this.search();
  }

  protected search(page = 0): void {
    if (this.form.invalid) return;
    const query = this.form.controls.query.value.trim();
    const kind = this.form.controls.kind.value;
    this.request?.unsubscribe();
    this.error.set('');
    void this.router.navigate([], {
      relativeTo: this.route, replaceUrl: true, queryParamsHandling: 'merge',
      queryParams: { busqueda: query || null, tipoBusqueda: kind, seccion: 'search' }
    });
    if (!query) {
      this.result.set(null);
      this.busy.set(false);
      return;
    }
    if (query !== this.lastQuery || kind !== this.lastKind) {
      page = 0;
      this.result.set(null);
    }
    this.lastQuery = query;
    this.lastKind = kind;
    this.busy.set(true);
    this.request = this.service.search(this.groupId(), query, kind, page).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))
    ).subscribe({
      next: result => this.result.set(result),
      error: error => this.error.set(getApiErrorMessage(error, 'No se pudo realizar la búsqueda.'))
    });
  }

  protected targetParams(result: CommunitySearchResult) {
    const target = this.types[result.kind];
    return {
      seccion: target.section, publicacion: null, tarea: null, evento: null, album: null,
      [target.param]: result.id, busqueda: this.lastQuery, tipoBusqueda: this.lastKind
    };
  }
}
