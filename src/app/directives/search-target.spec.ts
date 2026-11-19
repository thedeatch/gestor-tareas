import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { SearchTarget } from './search-target';

@Component({
  imports: [SearchTarget],
  template: '<article [appSearchTarget]="7" searchTargetParam="tarea">Tarea encontrada</article>'
})
class SearchTargetHost {}

describe('SearchTarget', () => {
  it('focuses the matching result after rendering and ignores another resource type', async () => {
    const params = new BehaviorSubject(convertToParamMap({ publicacion: '7' }));
    await TestBed.configureTestingModule({
      imports: [SearchTargetHost],
      providers: [{ provide: ActivatedRoute, useValue: { queryParamMap: params.asObservable() } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(SearchTargetHost);
    fixture.detectChanges();
    const article = fixture.nativeElement.querySelector('article') as HTMLElement;
    const scroll = vi.fn();
    article.scrollIntoView = scroll;
    const focus = vi.spyOn(article, 'focus');
    expect(scroll).not.toHaveBeenCalled();
    params.next(convertToParamMap({ tarea: '7' }));
    fixture.detectChanges();
    expect(scroll).toHaveBeenCalledWith({ block: 'center' });
    expect(focus).toHaveBeenCalled();
    expect(article.tabIndex).toBe(-1);
  });
});
