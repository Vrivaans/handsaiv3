import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToolsBatch } from './tools-batch';

describe('ToolsBatch', () => {
  let component: ToolsBatch;
  let fixture: ComponentFixture<ToolsBatch>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolsBatch]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToolsBatch);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
