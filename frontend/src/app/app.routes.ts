import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { SlowQueriesComponent } from './features/slow-queries/slow-queries.component';
import { QueryShapesComponent } from './features/query-shapes/query-shapes.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'slow-queries', component: SlowQueriesComponent },
  { path: 'query-shapes', component: QueryShapesComponent },
];
