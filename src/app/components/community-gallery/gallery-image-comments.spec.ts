import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GalleryImageComments } from './gallery-image-comments';
import { provideRouter } from '@angular/router';

describe('GalleryImageComments', () => {
  const url = '/api/communities/2/albums/1/images/3/comments';
  const comment = {
    id: 1, authorId: 2, authorName: 'María', authorHasAvatar: false,
    content: 'Qué buena foto', createdAt: '2026-11-10T20:30:00Z', likeCount: 0, liked: false
  };
  let fixture: ComponentFixture<GalleryImageComments>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GalleryImageComments],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(GalleryImageComments);
    fixture.componentRef.setInput('url', url);
    fixture.detectChanges();
    http.expectOne(url).flush([]);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function submitComment(): void {
    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    textarea.value = comment.content;
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
  }

  it('publishes through the Comentar button and renders the saved comment and reaction', () => {
    const counts: number[] = [];
    fixture.componentInstance.commentCountChanged.subscribe(count => counts.push(count));
    submitComment();
    const request = http.expectOne({ method: 'POST', url });
    expect(request.request.body).toEqual({ content: comment.content });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    request.flush(comment);
    expect(counts).toEqual([1]);
    fixture.detectChanges();
    const article = fixture.nativeElement.querySelector('article') as HTMLElement;
    expect(article.textContent).toContain('María');
    expect(article.querySelector('.author-link')?.getAttribute('href')).toBe('/usuarios/2');
    expect(article.querySelector('.avatar-link')?.getAttribute('href')).toBe('/usuarios/2');
    expect(article.textContent).toContain(comment.content);
    expect(fixture.nativeElement.querySelector('textarea').value).toBe('');
    (article.querySelector('button') as HTMLButtonElement).click();
    http.expectOne({ method: 'PUT', url: url + '/1/like' })
      .flush({ ...comment, liked: true, likeCount: 1 });
    fixture.detectChanges();
    expect(article.querySelector('button')?.getAttribute('aria-pressed')).toBe('true');
  });

  it('preserves the draft and allows retry after a failed publication', () => {
    submitComment();
    http.expectOne({ method: 'POST', url }).flush({}, { status: 500, statusText: 'Server error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent)
      .toContain('No se pudo publicar el comentario.');
    expect(fixture.nativeElement.querySelector('textarea').value).toBe(comment.content);
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    http.expectOne({ method: 'POST', url }).flush(comment);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('article').textContent).toContain(comment.content);
  });
});
