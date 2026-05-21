import { Component } from '@angular/core';
import { RouterModule, RouterOutlet } from '@angular/router';
import { PRIME_COMPONENTS } from './shared/shared-primeng';
import { CommonModule } from '@angular/common';
import { SystemMessageService } from './shared/services/system-message.service';
import { ConfirmationService } from 'primeng/api';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterModule, PRIME_COMPONENTS, CommonModule],
  providers: [SystemMessageService, ConfirmationService],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  title = 'frontend';

  confirmDialogHide() {
    console.log('Global ConfirmDialog Closed');
  }
}
