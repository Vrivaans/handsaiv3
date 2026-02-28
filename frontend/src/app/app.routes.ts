import { Routes } from '@angular/router';
import { HomeComponent } from './home/home';
import { ToolsComponent } from './tools/tools';
import { AnalyticsComponent } from './analytics/analytics';

export const routes: Routes = [
    { path: 'home', component: HomeComponent },
    { path: 'tools', component: ToolsComponent },
    { path: 'analytics', component: AnalyticsComponent },
    { path: '', redirectTo: '/home', pathMatch: 'full' },
    { path: '**', redirectTo: '/home' }
];
