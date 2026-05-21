import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SettingManageComponent } from './setting-manage.component';

describe('SettingManageComponent', () => {
  let component: SettingManageComponent;
  let fixture: ComponentFixture<SettingManageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingManageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SettingManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
