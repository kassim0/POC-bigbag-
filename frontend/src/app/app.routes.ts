import { Routes } from '@angular/router';
import { Home } from './home/home';
import { ListDetail } from './list-detail/list-detail';

export const routes: Routes = [
  { path: '', component: Home, title: 'BingBang – My games wallet' },
  { path: 'lists/:id', component: ListDetail, title: 'BingBang – Games list' },
  { path: '**', redirectTo: '' },
];
