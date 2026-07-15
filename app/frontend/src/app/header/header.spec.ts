import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Header } from './header';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the Play logo', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const logo = compiled.querySelector<HTMLImageElement>('img.header__logo');
    expect(logo).toBeTruthy();
    expect(logo?.getAttribute('src')).toBe('/logo.svg');
    expect(logo?.getAttribute('alt')).toBe('Play');
  });

  it('should render inside a banner landmark', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('header[role="banner"]')).toBeTruthy();
  });
});
