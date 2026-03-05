import { Routes } from '@angular/router';
import { HomeComponent } from './home/home';
import { ToolsComponent } from './tools/tools';
import { AnalyticsComponent } from './analytics/analytics';
import { Tasks } from './tasks/tasks';
import { SetupComponent } from './auth/setup/setup';
import { LoginComponent } from './auth/login/login';
import { TokenManagementComponent } from './token-management/token-management';
import { authGuard } from './auth-guard';

export const routes: Routes = [
    { path: 'auth/setup', component: SetupComponent, canActivate: [authGuard] },
    { path: 'auth/login', component: LoginComponent, canActivate: [authGuard] },
    { path: 'home', component: HomeComponent, canActivate: [authGuard] },
    { path: 'tools', component: ToolsComponent, canActivate: [authGuard] },
    { path: 'analytics', component: AnalyticsComponent, canActivate: [authGuard] },
    { path: 'tasks', component: Tasks, canActivate: [authGuard] },
    { path: 'token', component: TokenManagementComponent, canActivate: [authGuard] },
    { path: '', redirectTo: '/home', pathMatch: 'full' },
    { path: '**', redirectTo: '/home' }
];
