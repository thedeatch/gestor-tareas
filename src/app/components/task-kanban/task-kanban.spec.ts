import { CdkDrag, CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { TaskStatus } from '../../models/task.model';
import { KanbanTask, TaskKanban } from './task-kanban';

describe('TaskKanban', () => {
  it('requests a column change when a card is dropped and blocks read-only cards', () => {
    TestBed.configureTestingModule({ imports: [TaskKanban], providers: [provideHttpClient()] });
    const fixture = TestBed.createComponent(TaskKanban);
    const task: KanbanTask = { id: 1, description: 'Tarea anterior', completed: false };
    fixture.componentRef.setInput('tasks', [task, {
      id: 2, description: 'Solo lectura', status: 'COMPLETED', canChangeStatus: false
    }]);
    fixture.componentRef.setInput('taskEndpoint', '/api/tasks');
    fixture.detectChanges();
    const changes: unknown[] = [];
    fixture.componentInstance.moved.subscribe(move => changes.push(move));
    const columns = fixture.debugElement.queryAll(By.directive(CdkDropList));
    const cards = fixture.debugElement.queryAll(By.directive(CdkDrag));
    const source = columns[0].injector.get(CdkDropList);
    const destination = columns[1].injector.get(CdkDropList);
    const drag = cards[0].injector.get(CdkDrag);
    const event: CdkDragDrop<TaskStatus, TaskStatus, KanbanTask> = {
      item: drag, container: destination, previousContainer: source,
      previousIndex: 0, currentIndex: 0, isPointerOverContainer: true,
      distance: { x: 100, y: 0 }, dropPoint: { x: 100, y: 0 }, event: new MouseEvent('mouseup')
    };
    destination.dropped.emit(event);
    expect(changes).toEqual([{ id: 1, status: 'IN_PROGRESS' }]);
    expect(cards[1].injector.get(CdkDrag).disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('select')).toBeNull();

    fixture.componentRef.setInput('busy', true);
    fixture.detectChanges();
    destination.dropped.emit(event);
    expect(changes.length).toBe(1);
  });
});
