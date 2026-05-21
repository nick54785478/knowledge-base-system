import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/pages/home/home.component';
import { KnowledgeManagementComponent } from './features/knowledge-management/pages/knowledge-management/knowledge-management.component';
import { LayoutComponent } from './features/layout/layout/layout.component';
import { SettingManageComponent } from './features/setting/pages/setting-manage/setting-manage.component';
import { RagAssistantComponent } from './features/rag-assistant/pages/rag-assistant/rag-assistant.component';

/**
 * url 路徑 Component 導向配置
 */
export const routes: Routes = [
  //   {
  //     path: '',
  //     component: KnowledgeManagementComponent,
  //   },

  {
    path: '',
    component: LayoutComponent, // Layout 作為殼
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      { path: 'home', component: HomeComponent },
      { path: 'manage', component: KnowledgeManagementComponent },
      { path: 'setting', component: SettingManageComponent },
      { path: 'rag-assistant', component: RagAssistantComponent },
    ],
  },
  { path: '**', redirectTo: 'chat' }, // 找不到路由時的防護
];
