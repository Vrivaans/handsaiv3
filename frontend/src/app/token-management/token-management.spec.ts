import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TokenManagement } from './token-management';

describe('TokenManagement', () => {
  let component: TokenManagement;
  let fixture: ComponentFixture<TokenManagement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TokenManagement]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TokenManagement);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
