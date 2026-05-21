import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PRIME_COMPONENTS } from '../../../../shared/shared-primeng';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, PRIME_COMPONENTS],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {}
