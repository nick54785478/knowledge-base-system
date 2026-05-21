import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import Aura from '@primeng/themes/aura'; // 這是 PrimeNG v18 最新、最漂亮的預設主題
import { provideHttpClient } from '@angular/common/http';
import { provideClientHydration } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
    // provideAnimations(),
    provideHttpClient(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura, // 👈 這裡就決定了你的 UI 長相
        options: {
          darkModeSelector: '.my-app-dark', // 設定切換深色模式的 class
        },
      },
    }),
    MessageService, // 💡 負責 p-toast
    ConfirmationService, // 💡 負責 p-confirmDialog
  ],
};
