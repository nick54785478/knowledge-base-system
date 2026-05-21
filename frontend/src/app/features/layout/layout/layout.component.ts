import { CommonModule } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/internal/operators/filter';
import { Subscription } from 'rxjs/internal/Subscription';
import { SidebarComponent } from './sidebar/sidebar.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private routerSub!: Subscription;

  // 控制手機版側邊欄是否展開的狀態
  isSidebarActive: boolean = false;

  ngOnInit() {
    // 監聽路由變化，當導航結束時自動收起手機版側邊欄
    this.routerSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.closeSidebar());
  }

  ngOnDestroy() {
    if (this.routerSub) {
      this.routerSub.unsubscribe();
    }
  }

  // 切換側邊欄狀態
  toggleSidebar() {
    this.isSidebarActive = !this.isSidebarActive;
  }

  // 強制關閉側邊欄
  closeSidebar() {
    this.isSidebarActive = false;
  }
}
