import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { AdminAuditPage } from './admin-audit-page';

describe('AdminAuditPage', () => {
  it('should create', () => {
    TestBed.configureTestingModule({
      imports: [AdminAuditPage],
      providers: [provideHttpClient()]
    });

    const fixture = TestBed.createComponent(AdminAuditPage);
    expect(fixture.componentInstance).toBeTruthy();
  });
});
