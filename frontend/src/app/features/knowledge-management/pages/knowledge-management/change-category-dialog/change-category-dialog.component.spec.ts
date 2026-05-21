import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChangeCategoryDialogComponent } from './change-category-dialog.component';

describe('ChangeCategoryDialogComponent', () => {
  let component: ChangeCategoryDialogComponent;
  let fixture: ComponentFixture<ChangeCategoryDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChangeCategoryDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChangeCategoryDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
