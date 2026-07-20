import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { TripService } from '../../core/services/trip.service';
import { DriverService } from '../../core/services/driver.service';
import { DashboardStats } from '../../core/models/dashboard.model';
import { Trip, TripStatus } from '../../core/models/trip.model';
import { DriverProfile } from '../../core/models/driver.model';
import { StatCardComponent } from '../shared/components/stat-card.component';
import { TripStatusChipComponent } from '../shared/components/trip-status-chip.component';
import { formatTripTimestamp } from '../../core/utils/date-format.util';

const ACTIVE_STATUSES: TripStatus[] = ['ACCEPTED', 'ARRIVED', 'IN_PROGRESS'];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatProgressSpinnerModule,
    MatIconModule,
    MatCardModule,
    MatButtonModule,
    StatCardComponent,
    TripStatusChipComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly authService = inject(AuthService);
  private readonly tripService = inject(TripService);
  private readonly driverService = inject(DriverService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly stats = signal<DashboardStats | null>(null);
  readonly recentTrips = signal<Trip[]>([]);
  readonly driverProfile = signal<DriverProfile | null>(null);
  readonly activeDriverTrip = signal<Trip | null>(null);

  readonly currentUser = this.authService.currentUser;
  readonly currentRole = this.authService.currentRole;
  readonly formatTimestamp = formatTripTimestamp;

  ngOnInit(): void {
    const role = this.authService.currentRole();

    if (role === 'ADMIN') {
      this.dashboardService.getDashboardStats().subscribe({
        next: (stats) => {
          this.stats.set(stats);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Failed to load dashboard stats');
          this.loading.set(false);
        }
      });
      return;
    }

    if (role === 'RIDER') {
      this.tripService.getMyTrips(0, 5).subscribe({
        next: (page) => {
          this.recentTrips.set(page.content);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
      return;
    }

    if (role === 'DRIVER') {
      this.driverService.getMyDriverProfile().subscribe({
        next: (profile) => {
          this.driverProfile.set(profile);
          this.tripService.getDriverTrips(profile.id, 0, 5).subscribe({
            next: (page) => {
              this.recentTrips.set(page.content);
              this.activeDriverTrip.set(
                page.content.find((t) => ACTIVE_STATUSES.includes(t.status)) ?? null
              );
              this.loading.set(false);
            },
            error: () => this.loading.set(false)
          });
        },
        error: () => this.loading.set(false)
      });
      return;
    }

    this.loading.set(false);
  }

  openTrip(trip: Trip): void {
    this.router.navigate(['/trips', trip.id]);
  }

  isOnTrip(): boolean {
    return this.driverProfile()?.status === 'ON_TRIP';
  }
}
