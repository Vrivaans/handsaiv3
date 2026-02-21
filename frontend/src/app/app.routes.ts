import { Routes } from '@angular/router';
import { HomeComponent } from './home/home';
import { ToolsComponent } from './tools/tools';
import { ToolsBatchComponent } from './tools-batch/tools-batch';
import { AnalyticsComponent } from './analytics/analytics';

export const routes: Routes = [
    { path: 'home', component: HomeComponent },
    { path: 'tools', component: ToolsComponent },
    { path: 'tools/batch', component: ToolsBatchComponent },
    { path: 'analytics', component: AnalyticsComponent },
    { path: '', redirectTo: '/home', pathMatch: 'full' },
    { path: '**', redirectTo: '/home' }
];
