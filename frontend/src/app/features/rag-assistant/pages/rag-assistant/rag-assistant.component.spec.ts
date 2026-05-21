import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RagAssistantComponent } from './rag-assistant.component';

describe('RagAssistantComponent', () => {
  let component: RagAssistantComponent;
  let fixture: ComponentFixture<RagAssistantComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RagAssistantComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RagAssistantComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
