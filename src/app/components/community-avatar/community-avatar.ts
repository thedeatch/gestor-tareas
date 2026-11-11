import { Component, effect, inject, input, OnDestroy } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Subscription } from 'rxjs';

import { CommunityService } from '../../services/community';

@Component({
  selector: 'app-community-avatar',
  imports: [MatIconModule],
  templateUrl: './community-avatar.html',
  styleUrl: './community-avatar.css'
})
export class CommunityAvatar implements OnDestroy {
  private readonly communityService = inject(CommunityService);
  private imageRequest?: Subscription;
  private objectUrl: string | null = null;

  readonly communityId = input.required<number>();
  readonly hasImage = input(false);
  readonly label = input('Grupo');

  protected imageUrl: string | null = null;

  constructor() {
    effect(() => {
      const id = this.communityId();
      const hasImage = this.hasImage();
      this.loadImage(id, hasImage);
    });
  }

  ngOnDestroy(): void {
    this.imageRequest?.unsubscribe();
    this.revokeObjectUrl();
  }

  private loadImage(id: number, hasImage: boolean): void {
    this.imageRequest?.unsubscribe();
    this.revokeObjectUrl();
    this.imageUrl = null;
    if (!hasImage) return;

    this.imageRequest = this.communityService.getImage(id).subscribe({
      next: blob => {
        this.revokeObjectUrl();
        this.objectUrl = URL.createObjectURL(blob);
        this.imageUrl = this.objectUrl;
      },
      error: () => this.imageUrl = null
    });
  }

  private revokeObjectUrl(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
    this.objectUrl = null;
  }
}
