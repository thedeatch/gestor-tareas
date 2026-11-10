import { Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface GalleryDialogImage { id: number; name: string; url: string; }
export interface GalleryImageDialogData {
  albumTitle: string;
  images: GalleryDialogImage[];
  initialIndex: number;
}

@Component({
  selector: 'app-gallery-image-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <header mat-dialog-title>
      <div><strong>{{ data.albumTitle }}</strong><span>{{ current().name }}</span></div>
      <button mat-icon-button type="button" aria-label="Cerrar visor" (click)="dialogRef.close()">
        <mat-icon>close</mat-icon>
      </button>
    </header>
    <mat-dialog-content>
      <button mat-icon-button type="button" aria-label="Imagen anterior"
        [disabled]="data.images.length < 2" (click)="previous()"><mat-icon>chevron_left</mat-icon></button>
      <figure><img [src]="current().url" [alt]="current().name"></figure>
      <button mat-icon-button type="button" aria-label="Imagen siguiente"
        [disabled]="data.images.length < 2" (click)="next()"><mat-icon>chevron_right</mat-icon></button>
    </mat-dialog-content>
    <footer>{{ index() + 1 }} de {{ data.images.length }}</footer>
  `,
  styles: [`
    header{display:flex;align-items:center;justify-content:space-between;gap:1rem}header div{display:flex;min-width:0;flex-direction:column}header span{color:var(--mat-sys-on-surface-variant);font-size:.85rem;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}mat-dialog-content{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:.5rem;padding-inline:.5rem!important}figure{display:grid;place-items:center;min-width:0;min-height:260px;margin:0}img{max-width:100%;max-height:70vh;border-radius:12px;object-fit:contain}footer{padding:.5rem 1.5rem 1rem;color:var(--mat-sys-on-surface-variant);text-align:center}@media(max-width:560px){mat-dialog-content{grid-template-columns:1fr}mat-dialog-content>button{position:absolute;z-index:1;background:var(--mat-sys-surface-container-high)}mat-dialog-content>button:first-child{left:.5rem}mat-dialog-content>button:last-child{right:.5rem}figure{min-height:200px}}
  `]
})
export class GalleryImageDialog {
  readonly data = inject<GalleryImageDialogData>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<GalleryImageDialog>);
  readonly index = signal(this.data.initialIndex);
  readonly current = computed(() => this.data.images[this.index()]);
  previous(): void { this.index.update(value => (value - 1 + this.data.images.length) % this.data.images.length); }
  next(): void { this.index.update(value => (value + 1) % this.data.images.length); }
}
