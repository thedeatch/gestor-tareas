import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CommunitySearch } from './community-search';

describe('CommunitySearch', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommunitySearch],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function create() {
    const fixture = TestBed.createComponent(CommunitySearch);
    fixture.componentRef.setInput('groupId', 2);
    fixture.detectChanges();
    return fixture;
  }

  function query(root: HTMLElement, value: string) {
    const input = root.querySelector('input')!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function matches(text: string, kind = 'ALL', page = 0) {
    return (request: HttpRequest<unknown>) => request.url === '/api/communities/2/search'
      && request.params.get('q') === text && request.params.get('kind') === kind
      && request.params.get('page') === String(page);
  }

  it('waits for a query and renders links to the specific resources', () => {
    const fixture = create();
    const root = fixture.nativeElement as HTMLElement;
    http.expectNone(request => request.url.endsWith('/search'));
    query(root, 'reunión Angular');
    fixture.detectChanges();
    root.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    http.expectOne(matches('reunión Angular')).flush({
      query: 'reunión Angular', total: 4, page: 0, size: 20,
      items: ['POST', 'TASK', 'EVENT', 'ALBUM'].map((kind, index) =>
        ({ kind, id: index + 1, title: kind + ' Angular', excerpt: 'Reunión' }))
    });
    fixture.detectChanges();
    const links = Array.from(root.querySelectorAll<HTMLAnchorElement>('.search-results a'));
    expect(links[0].getAttribute('href')).toContain('seccion=posts&publicacion=1');
    expect(links[1].getAttribute('href')).toContain('seccion=productivity&tarea=2');
    expect(links[2].getAttribute('href')).toContain('seccion=events&evento=3');
    expect(links[3].getAttribute('href')).toContain('seccion=events&album=4');
    expect(root.textContent).toContain('4 resultados');
  });

  it('changes the content filter and cancels an obsolete request', () => {
    const fixture = create();
    const root = fixture.nativeElement as HTMLElement;
    query(root, 'Angular');
    fixture.detectChanges();
    root.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    const previous = http.expectOne(matches('Angular'));
    root.querySelector<HTMLElement>('.mat-mdc-select-trigger')!.click();
    fixture.detectChanges();
    Array.from(document.querySelectorAll<HTMLElement>('mat-option'))
      .find(option => option.textContent?.includes('Álbumes'))!.click();
    fixture.detectChanges();
    expect(previous.cancelled).toBe(true);
    http.expectOne(matches('Angular', 'ALBUM')).flush({
      query: 'Angular', items: [], total: 0, page: 0, size: 20
    });
    fixture.detectChanges();
    expect(root.textContent).toContain('No encontramos coincidencias');
  });

  it('preserves the query after an error and paginates a successful retry', () => {
    const fixture = create();
    const root = fixture.nativeElement as HTMLElement;
    query(root, 'Angular');
    fixture.detectChanges();
    root.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    http.expectOne(matches('Angular')).flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(root.querySelector('input')!.value).toBe('Angular');
    root.querySelector<HTMLButtonElement>('.error button')!.click();
    http.expectOne(matches('Angular')).flush({
      query: 'Angular', total: 21, page: 0, size: 20,
      items: [{ kind: 'POST', id: 1, title: 'Angular', excerpt: 'Contenido' }]
    });
    fixture.detectChanges();
    root.querySelectorAll<HTMLButtonElement>('.pagination button')[1].click();
    http.expectOne(matches('Angular', 'ALL', 1)).flush({
      query: 'Angular', total: 21, page: 1, size: 20,
      items: [{ kind: 'POST', id: 2, title: 'Otro resultado', excerpt: 'Contenido' }]
    });
    fixture.detectChanges();
    expect(root.textContent).toContain('Página 2');
  });

  it('clears the remembered query when the empty form is submitted', () => {
    const fixture = create();
    const root = fixture.nativeElement as HTMLElement;
    query(root, '  ');
    root.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    http.expectNone(request => request.url.endsWith('/search'));
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith([], expect.objectContaining({
      queryParams: { busqueda: null, tipoBusqueda: 'ALL', seccion: 'search' }
    }));
  });
});
