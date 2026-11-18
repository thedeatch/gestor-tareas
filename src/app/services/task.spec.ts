import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { TaskService } from './task';
import { Task, TaskDetails } from '../models/task.model';

describe('TaskService', () => {
  let service: TaskService;
  let httpTesting: HttpTestingController;
  it('should create a task through the API', () => {
    const initialRequest =
      httpTesting.expectOne(apiUrl);

    initialRequest.flush([]);

    const createdTask: Task = {
      id: 10,
      description: 'Probar POST desde Angular',
      completed: false
    };

    service
      .addTask('Probar POST desde Angular')
      .subscribe(result => {
        expect(result).toEqual(createdTask);
      });

    const postRequest =
      httpTesting.expectOne(apiUrl);

    expect(postRequest.request.method).toBe('POST');

    expect(postRequest.request.body).toEqual({
      description: 'Probar POST desde Angular',
      completed: false
    });

    postRequest.flush(
      createdTask,
      {
        status: 201,
        statusText: 'Created'
      }
    );

    expect(service.tasks()).toEqual([
      createdTask
    ]);
  });
  it('should toggle a task through the API', () => {
    const initialTask: Task = {
      id: 1,
      description: 'Tarea pendiente',
      completed: false
    };

    const initialRequest =
      httpTesting.expectOne(apiUrl);

    initialRequest.flush([initialTask]);

    const updatedTask: Task = {
      ...initialTask,
      completed: true
    };

    service.toggleTask(initialTask.id)
      .subscribe(result => {
        expect(result).toEqual(updatedTask);
      });

    const putRequest = httpTesting.expectOne(
      `${apiUrl}/${initialTask.id}`
    );

    expect(putRequest.request.method).toBe('PUT');

    expect(putRequest.request.body).toEqual(
      updatedTask
    );

    putRequest.flush(updatedTask);

    expect(service.tasks()).toEqual([
      updatedTask
    ]);

  });

  it('should update task details and preserve the project and category', () => {
    const initialTask: Task = {
      id: 2,
      description: 'Descripción original',
      completed: false,
      projectId: 4,
      categoryId: 5
    };

    const initialRequest =
      httpTesting.expectOne(apiUrl);

    initialRequest.flush([initialTask]);

    const details: TaskDetails = {
      description: 'Descripción actualizada',
      status: 'COMPLETED',
      priority: 'HIGH',
      dueDate: '2026-11-20',
      tags: ['revisión']
    };
    const updatedTask: Task = { id: initialTask.id, completed: true, ...details };
    service.updateTaskDetails(initialTask.id, details).subscribe(result => {
      expect(result).toEqual(updatedTask);
    });

    const putRequest = httpTesting.expectOne(
      `${apiUrl}/${initialTask.id}/details`
    );

    expect(putRequest.request.method).toBe('PUT');

    expect(putRequest.request.body).toEqual(
      details
    );

    putRequest.flush(updatedTask);

    expect(service.tasks()).toEqual([
      { ...updatedTask, projectId: 4, categoryId: 5 }
    ]);

    expect(
      service.tasks()[0].description
    ).toBe('Descripción actualizada');
  });
  it('should delete a task through the API', () => {
    const existingTask: Task = {
      id: 3,
      description: 'Tarea que será eliminada',
      completed: false
    };

    const initialRequest =
      httpTesting.expectOne(apiUrl);

    initialRequest.flush([existingTask]);

      service.deleteTask(existingTask.id)
        .subscribe();

    const deleteRequest = httpTesting.expectOne(
      `${apiUrl}/${existingTask.id}`
    );

    expect(deleteRequest.request.method)
      .toBe('DELETE');

    expect(service.tasks()).toEqual([
      existingTask
    ]);

    deleteRequest.flush(
      null,
      {
        status: 204,
        statusText: 'No Content'
      }
    );

    expect(service.tasks()).toEqual([]);
  });
  const apiUrl =
    '/api/tasks';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(TaskService);
    httpTesting = TestBed.inject(
      HttpTestingController
    );
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    const request = httpTesting.expectOne(apiUrl);
    request.flush([]);

    expect(service).toBeTruthy();
  });

  it('should load tasks from the API', () => {
    const expectedTasks: Task[] = [
      {
        id: 1,
        description: 'Aprender pruebas HTTP',
        completed: false
      }
    ];

    const request = httpTesting.expectOne(apiUrl);

    expect(request.request.method).toBe('GET');

    request.flush(expectedTasks);

    expect(service.tasks()).toEqual(expectedTasks);
    expect(service.isLoading()).toBe(false);
    expect(service.loadError()).toBeNull();
  });

  it('should expose an error when loading fails', () => {
    const request = httpTesting.expectOne(apiUrl);

    request.flush(
      'Error del servidor',
      {
        status: 500,
        statusText: 'Internal Server Error'
      }
    );

    expect(service.tasks()).toEqual([]);
    expect(service.isLoading()).toBe(false);
    expect(service.loadError()).not.toBeNull();
  });
});
