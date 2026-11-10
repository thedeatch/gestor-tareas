import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CommunityOverview } from './community-overview';

describe('CommunityOverview', () => {
  let fixture: ComponentFixture<CommunityOverview>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CommunityOverview] }).compileComponents();
    fixture = TestBed.createComponent(CommunityOverview);
    fixture.componentRef.setInput('memberCount', 4);
    fixture.componentRef.setInput('postCount', 2);
    fixture.detectChanges();
  });

  it('muestra los totales y abre la sección seleccionada', () => {
    const emitted: string[] = [];
    fixture.componentInstance.sectionSelected.subscribe(section => emitted.push(section));

    const host = fixture.nativeElement as HTMLElement;
    const buttons = Array.from(host.querySelectorAll<HTMLButtonElement>('.overview-card'));
    expect(fixture.nativeElement.textContent).toContain('4 integrantes');
    expect(fixture.nativeElement.textContent).toContain('2 publicadas');

    buttons[0]!.click();
    expect(emitted).toEqual(['posts']);
  });

  it('solo muestra moderación a quienes pueden administrarla', () => {
    expect(fixture.nativeElement.textContent).not.toContain('Moderación');

    fixture.componentRef.setInput('canManage', true);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Moderación');
  });
});
