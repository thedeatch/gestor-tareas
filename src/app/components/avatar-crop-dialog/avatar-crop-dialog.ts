import {
  AfterViewInit,
  Component,
  ElementRef,
  ViewChild,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef
} from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface AvatarCropDialogData {
  sourceUrl: string;
}

@Component({
  selector: 'app-avatar-crop-dialog',
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule
  ],
  templateUrl: './avatar-crop-dialog.html',
  styleUrl: './avatar-crop-dialog.css'
})
export class AvatarCropDialog implements AfterViewInit {
  private static readonly OUTPUT_SIZE = 512;

  private readonly dialogRef = inject(MatDialogRef<AvatarCropDialog>);
  protected readonly data = inject<AvatarCropDialogData>(MAT_DIALOG_DATA);

  @ViewChild('canvas')
  private readonly canvas?: ElementRef<HTMLCanvasElement>;

  protected zoom = 1;
  protected horizontalPosition = 0;
  protected verticalPosition = 0;
  protected imageReady = false;
  protected isDragging = false;
  private image?: HTMLImageElement;
  private lastPointerX = 0;
  private lastPointerY = 0;

  ngAfterViewInit(): void {
    const image = new Image();
    image.onload = () => {
      this.image = image;
      this.imageReady = true;
      this.draw();
    };
    image.src = this.data.sourceUrl;
  }

  protected draw(): void {
    const canvas = this.canvas?.nativeElement;
    if (!canvas || !this.image) {
      return;
    }

    const size = AvatarCropDialog.OUTPUT_SIZE;
    const context = canvas.getContext('2d');
    if (!context) {
      return;
    }

    const baseScale = Math.max(
      size / this.image.naturalWidth,
      size / this.image.naturalHeight
    );
    const scale = baseScale * this.zoom;
    const width = this.image.naturalWidth * scale;
    const height = this.image.naturalHeight * scale;
    const maxHorizontalOffset = Math.max(0, (width - size) / 2);
    const maxVerticalOffset = Math.max(0, (height - size) / 2);
    const x = (size - width) / 2 +
      (this.horizontalPosition / 100) * maxHorizontalOffset;
    const y = (size - height) / 2 +
      (this.verticalPosition / 100) * maxVerticalOffset;

    context.clearRect(0, 0, size, size);
    context.drawImage(this.image, x, y, width, height);
  }

  protected startDragging(event: PointerEvent): void {
    const canvas = event.currentTarget as HTMLCanvasElement;
    canvas.setPointerCapture(event.pointerId);
    this.isDragging = true;
    this.lastPointerX = event.clientX;
    this.lastPointerY = event.clientY;
  }

  protected drag(event: PointerEvent): void {
    if (!this.isDragging) {
      return;
    }

    const canvas = event.currentTarget as HTMLCanvasElement;
    const rect = canvas.getBoundingClientRect();
    const deltaX = event.clientX - this.lastPointerX;
    const deltaY = event.clientY - this.lastPointerY;
    this.lastPointerX = event.clientX;
    this.lastPointerY = event.clientY;

    this.horizontalPosition = this.clamp(
      this.horizontalPosition + (deltaX / rect.width) * 200,
      -100,
      100
    );
    this.verticalPosition = this.clamp(
      this.verticalPosition + (deltaY / rect.height) * 200,
      -100,
      100
    );
    this.draw();
  }

  protected stopDragging(event: PointerEvent): void {
    const canvas = event.currentTarget as HTMLCanvasElement;
    if (canvas.hasPointerCapture(event.pointerId)) {
      canvas.releasePointerCapture(event.pointerId);
    }
    this.isDragging = false;
  }

  protected changeZoomWithWheel(event: WheelEvent): void {
    event.preventDefault();
    this.zoom = this.clamp(
      this.zoom - event.deltaY * 0.002,
      1,
      3
    );
    this.draw();
  }

  protected confirm(): void {
    const canvas = this.canvas?.nativeElement;
    if (!canvas) {
      return;
    }

    canvas.toBlob(blob => {
      if (!blob) {
        return;
      }
      this.dialogRef.close(new File(
        [blob],
        'avatar.webp',
        { type: 'image/webp' }
      ));
    }, 'image/webp', 0.9);
  }

  private clamp(value: number, minimum: number, maximum: number): number {
    return Math.min(maximum, Math.max(minimum, value));
  }
}
