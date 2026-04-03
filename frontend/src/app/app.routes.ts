import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { SlowQueriesComponent } from './features/slow-queries/slow-queries.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'slow-queries', component: SlowQueriesComponent },
];
