import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';

import { CommunitySummary, CommunityVisibility } from '../../models/community.model';
import { CommunityService } from '../../services/community';

@Component({
  selector: 'app-communities-page',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule,
    MatSnackBarModule],
  templateUrl: './communities-page.html',
  styleUrl: './communities-page.css'
})
export class CommunitiesPage {
  private readonly service = inject(CommunityService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly communities = signal<CommunitySummary[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly isCreating = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showForm = signal(false);
  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)] }),
    description: new FormControl('', { nonNullable: true,
      validators: [Validators.required, Validators.maxLength(500)] }),
    visibility: new FormControl<CommunityVisibility>('PUBLIC', {
      nonNullable: true, validators: [Validators.required]
    })
  });

  constructor() { this.load(); }

  protected load(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.service.findMine().pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: communities => this.communities.set(communities),
        error: () => this.error.set('No fue posible cargar tus grupos.')
      });
  }

  protected create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isCreating.set(true);
    this.service.create(this.form.getRawValue())
      .pipe(finalize(() => this.isCreating.set(false)))
      .subscribe({
        next: community => {
          this.communities.update(items => [community, ...items]);
          this.form.reset({ name: '', description: '', visibility: 'PUBLIC' });
          this.showForm.set(false);
          this.snackBar.open('Grupo creado correctamente.', 'Cerrar',
            { duration: 2500 });
        },
        error: () => this.snackBar.open('No fue posible crear la comunidad.',
          'Cerrar', { duration: 3000 })
      });
  }

  protected visibilityLabel(value: CommunityVisibility): string {
    return { PUBLIC: 'Pública', PRIVATE: 'Privada', HIDDEN: 'Oculta' }[value];
  }

  protected roleLabel(role: CommunitySummary['currentUserRole']): string {
    return { OWNER: 'Propietario', COMMUNITY_ADMIN: 'Administrador',
      COMMUNITY_MODERATOR: 'Moderador', MEMBER: 'Miembro' }[role];
  }
}
