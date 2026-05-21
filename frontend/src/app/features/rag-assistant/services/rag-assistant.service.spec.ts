import { TestBed } from '@angular/core/testing';

import { RagAssistantService } from './rag-assistant.service';

describe('RagAssistantService', () => {
  let service: RagAssistantService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RagAssistantService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
