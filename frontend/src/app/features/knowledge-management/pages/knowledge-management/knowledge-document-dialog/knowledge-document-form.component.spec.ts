import { ComponentFixture, TestBed } from '@angular/core/testing';

import { KnowledgeDocumentDialogComponent } from './knowledge-document-form.component';

describe('KnowledgeDocumentDialogComponent', () => {
  let component: KnowledgeDocumentDialogComponent;
  let fixture: ComponentFixture<KnowledgeDocumentDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KnowledgeDocumentDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(KnowledgeDocumentDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
