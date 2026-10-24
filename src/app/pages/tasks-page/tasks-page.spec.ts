import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import { TasksPage } from './tasks-page';

describe('TasksPage', () => {
  let component: TasksPage;
  let fixture: ComponentFixture<TasksPage>;
  let httpTesting: HttpTestingController;

  const apiUrl =
    '/api/tasks';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TasksPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    httpTesting = TestBed.inject(
      HttpTestingController
    );

    fixture = TestBed.createComponent(TasksPage);
    component = fixture.componentInstance;

    const request = httpTesting.expectOne(apiUrl);
    request.flush([]);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
