import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EditTaskDialog } from './edit-task-dialog';

describe('EditTaskDialog', () => {
  let component: EditTaskDialog;
  let fixture: ComponentFixture<EditTaskDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditTaskDialog],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            description: 'Tarea de prueba'
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EditTaskDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
