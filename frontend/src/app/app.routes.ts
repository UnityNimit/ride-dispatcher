import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent)
  },

  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/shared/layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'trips',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () => import('./features/trips/trip-list/trip-list.component').then((m) => m.TripListComponent)
      },
      {
        path: 'trips/:id',
        loadComponent: () =>
          import('./features/trips/trip-detail/trip-detail.component').then((m) => m.TripDetailComponent)
      },
      {
        path: 'drivers',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/drivers/driver-list/driver-list.component').then((m) => m.DriverListComponent)
      },
      {
        path: 'request-ride',
        canActivate: [roleGuard(['RIDER'])],
        loadComponent: () =>
          import('./features/trips/request-ride/request-ride.component').then((m) => m.RequestRideComponent)
      },
      {
        path: 'trip-history',
        canActivate: [roleGuard(['RIDER'])],
        loadComponent: () =>
          import('./features/trips/trip-history/trip-history.component').then((m) => m.TripHistoryComponent)
      },
      {
        path: 'incoming-requests',
        canActivate: [roleGuard(['DRIVER'])],
        loadComponent: () =>
          import('./features/drivers/incoming-request/incoming-request.component').then(
            (m) => m.IncomingRequestComponent
          )
      },
      {
        path: 'driver-trips',
        canActivate: [roleGuard(['DRIVER'])],
        loadComponent: () =>
          import('./features/drivers/driver-trips/driver-trips.component').then(
            (m) => m.DriverTripsComponent
          )
      }
    ]
  },

  { path: '**', redirectTo: 'dashboard' }
];
