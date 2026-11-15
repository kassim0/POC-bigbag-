import { Routes } from '@angular/router';
import { AccountPage } from './account/account';
import { Home } from './home/home';
import { ListDetail } from './list-detail/list-detail';

export const routes: Routes = [
  { path: '', component: Home, title: 'BingBang – My games wallet' },
  { path: 'lists/:id', component: ListDetail, title: 'BingBang – Games list' },
  { path: 'account', component: AccountPage, title: 'BingBang – Account' },
  { path: '**', redirectTo: '' },
];
