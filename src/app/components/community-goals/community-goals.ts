import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { Observable, finalize, switchMap, tap } from 'rxjs';
import { CommunityGoal, CommunityGoalOverview, CommunityTask } from '../../models/community-goal.model';
import { TASK_STATUS_LABELS, TaskStatus } from '../../models/task.model';
import { getApiErrorMessage } from '../../models/api-error.model';
import { CommunityGoalsService } from '../../services/community-goals';
import { ConfirmDialog, ConfirmDialogData } from '../confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-community-goals',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, MatButtonModule],
  templateUrl: './community-goals.html',
  styleUrl: './community-goals.css'
})
export class CommunityGoals implements OnInit {
  readonly groupId = input.required<number>();
  private readonly service = inject(CommunityGoalsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  protected readonly overview = signal<CommunityGoalOverview | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly editorOpen = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly statuses = Object.entries(TASK_STATUS_LABELS) as [TaskStatus, string][];
  protected readonly statusLabels = TASK_STATUS_LABELS;
  protected readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(2000)] }),
    dueDate: new FormControl('', { nonNullable: true, validators: [Validators.pattern(/^\d{4}-\d{2}-\d{2}$/)] }),
    taskIds: new FormControl<number[]>([], { nonNullable: true })
  });

  ngOnInit(): void { this.load(); }
  protected load(): void { this.run(this.service.findAll(this.groupId()), 'No se pudieron cargar los objetivos.'); }
  protected edit(goal?: CommunityGoal): void {
    if (this.busy()) return;
    this.editingId.set(goal?.id ?? null);
    this.form.reset({ title: goal?.title ?? '', description: goal?.description ?? '', dueDate: goal?.dueDate ?? '', taskIds: goal ? [...goal.taskIds] : [] });
    this.editorOpen.set(true);
  }
  protected cancel(): void { this.editorOpen.set(false); this.editingId.set(null); }
  protected toggleTask(id: number, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const control = this.form.controls.taskIds;
    control.setValue(checked ? [...new Set([...control.value, id])] : control.value.filter(value => value !== id));
  }
  protected save(): void {
    if (this.busy()) return;
    if (!this.form.controls.title.value.trim()) this.form.controls.title.setErrors({ required: true });
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    const draft = { ...value, title: value.title.trim(), dueDate: value.dueDate || null };
    const id = this.editingId();
    this.run((id === null ? this.service.create(this.groupId(), draft) : this.service.update(this.groupId(), id, draft))
      .pipe(tap(() => this.cancel())), 'No se pudo guardar el objetivo.');
  }
  protected linkedTasks(goal: CommunityGoal): CommunityTask[] {
    return (this.overview()?.tasks ?? []).filter(task => goal.taskIds.includes(task.id));
  }
  protected changeStatus(task: CommunityTask, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const status = select.value as TaskStatus;
    select.value = task.status;
    if (this.busy() || !task.canChangeStatus || status === task.status) return;
    this.run(this.service.changeTaskStatus(this.groupId(), task.id, status).pipe(
      switchMap(() => this.service.findAll(this.groupId()).pipe(tap({ error: () =>
        this.error.set('El estado se guardó. Pulsa Actualizar para consultar el avance.') })))
    ), 'No se pudo cambiar el estado de la tarea.');
  }
  protected remove(goal: CommunityGoal): void {
    if (this.busy()) return;
    this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
      data: { title: 'Eliminar objetivo', itemLabel: 'el objetivo', itemName: goal.title, confirmLabel: 'Eliminar' }
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(confirmed => {
      if (confirmed) this.run(this.service.delete(this.groupId(), goal.id).pipe(tap(() => {
        if (this.editingId() === goal.id) this.cancel();
      })), 'No se pudo eliminar el objetivo.');
    });
  }
  private run(request: Observable<CommunityGoalOverview>, fallback: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.form.disable();
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      this.busy.set(false); this.form.enable();
    })).subscribe({
      next: data => {
        this.overview.set(data);
        const available = new Set(data.tasks.map(task => task.id));
        this.form.controls.taskIds.setValue(this.form.controls.taskIds.value.filter(id => available.has(id)));
      },
      error: error => this.error.update(current => current || getApiErrorMessage(error, fallback))
    });
  }
}
