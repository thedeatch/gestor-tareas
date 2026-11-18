import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Observable, finalize, map, timer } from 'rxjs';
import { CommunityPoll, CommunityPollOption } from '../../models/community-poll.model';
import { getApiErrorMessage } from '../../models/api-error.model';
import { CommunityPollsService } from '../../services/community-polls';
import { ConfirmDialog, ConfirmDialogData } from '../confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-community-polls',
  imports: [DatePipe, RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './community-polls.html',
  styleUrl: './community-polls.css'
})
export class CommunityPolls implements OnInit {
  readonly groupId = input.required<number>();
  private readonly service = inject(CommunityPollsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly now = toSignal(timer(0, 30_000).pipe(map(() => Date.now())), { initialValue: Date.now() });
  protected readonly polls = signal<CommunityPoll[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly creating = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly form = new FormGroup({
    question: new FormControl('', {
      nonNullable: true, validators: [Validators.required, Validators.maxLength(300)]
    }),
    options: new FormArray([this.optionControl(), this.optionControl()]),
    closesAt: new FormControl('', { nonNullable: true })
  });
  protected readonly options = this.form.controls.options;

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.run(this.service.findAll(this.groupId()), polls => this.polls.set(polls),
      'No se pudieron cargar las encuestas.');
  }

  protected addOption(): void {
    if (this.options.length < 10) this.options.push(this.optionControl());
  }

  protected removeOption(index: number): void {
    if (this.options.length > 2) this.options.removeAt(index);
  }

  protected create(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy()) return;
    const value = this.form.getRawValue();
    const question = value.question.trim();
    const options = value.options.map(option => option.trim());
    if (!question || options.some(option => !option)) {
      this.error.set('Escribe una pregunta y completa todas las opciones.');
      return;
    }
    if (new Set(options.map(option => option.toLocaleLowerCase())).size !== options.length) {
      this.error.set('Las opciones deben ser diferentes.');
      return;
    }
    const date = value.closesAt ? new Date(value.closesAt) : null;
    if (date && (!Number.isFinite(date.getTime()) || date.getTime() <= Date.now())) {
      this.error.set('La fecha de cierre debe ser futura.');
      return;
    }
    this.run(this.service.create(this.groupId(), {
      question, options, closesAt: date?.toISOString() ?? null
    }), poll => {
      this.polls.update(items => [poll, ...(items ?? [])]);
      this.creating.set(false);
      this.form.reset();
      while (this.options.length > 2) this.options.removeAt(this.options.length - 1);
      this.message.set('Encuesta publicada.');
    }, 'No se pudo publicar la encuesta.');
  }

  protected isClosed(poll: CommunityPoll): boolean {
    return poll.closed || (poll.closesAt !== null && Date.parse(poll.closesAt) <= this.now());
  }

  protected percentage(poll: CommunityPoll, option: CommunityPollOption): number {
    return poll.totalVotes ? Math.round(option.votes * 100 / poll.totalVotes) : 0;
  }

  protected vote(poll: CommunityPoll, option: CommunityPollOption): void {
    if (this.isClosed(poll) || poll.selectedOptionId === option.id) return;
    this.run(this.service.vote(this.groupId(), poll.id, option.id), updated => {
      this.replace(updated);
      this.message.set('Voto guardado.');
    }, 'No se pudo guardar el voto. Actualiza las encuestas para consultar su estado.');
  }

  protected close(poll: CommunityPoll): void {
    const data: ConfirmDialogData = {
      title: 'Cerrar encuesta',
      itemLabel: 'la encuesta',
      itemName: poll.question,
      confirmLabel: 'Cerrar encuesta',
      action: 'cerrar'
    };
    this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, { data })
      .afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(confirmed => {
        if (!confirmed) return;
        this.run(this.service.close(this.groupId(), poll.id), updated => {
          this.replace(updated);
          this.message.set('Encuesta cerrada.');
        }, 'No se pudo cerrar la encuesta.');
      });
  }

  private replace(poll: CommunityPoll): void {
    this.polls.update(items => items?.map(item => item.id === poll.id ? poll : item) ?? null);
  }

  private optionControl(): FormControl<string> {
    return new FormControl('', {
      nonNullable: true, validators: [Validators.required, Validators.maxLength(200)]
    });
  }

  private run<T>(request: Observable<T>, success: (result: T) => void, fallback: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: success,
      error: error => this.error.set(getApiErrorMessage(error, fallback))
    });
  }
}
