import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, startWith, switchMap, catchError, of } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { TripService } from '../../../core/services/trip.service';
import { DriverService } from '../../../core/services/driver.service';
import { Trip } from '../../../core/models/trip.model';
import { DriverProfile, DriverStatus } from '../../../core/models/driver.model';
import { formatTripTimestamp } from '../../../core/utils/date-format.util';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-incoming-request',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    RouterLink
  ],
  templateUrl: './incoming-request.component.html',
  styleUrl: './incoming-request.component.scss'
})
export class IncomingRequestComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly acceptingId = signal<number | null>(null);
  readonly goingOnline = signal(false);
  readonly updatingLocation = signal(false);
  readonly driver = signal<DriverProfile | null>(null);
  readonly requests = signal<Trip[]>([]);
  readonly formatTimestamp = formatTripTimestamp;

  private readonly tripService = inject(TripService);
  private readonly driverService = inject(DriverService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  ngOnInit(): void {
    this.driverService.getMyDriverProfile().subscribe({
      next: (profile) => this.driver.set(profile),
      error: () => this.snackBar.open('Could not load driver profile', 'Dismiss', { duration: 3000 })
    });

    interval(environment.pollingIntervalMs)
      .pipe(
        startWith(0),
        switchMap(() => {
          const lat = this.driver()?.currentLat;
          const lng = this.driver()?.currentLng;
          if (lat == null || lng == null) {
            return of([] as Trip[]);
          }
          return this.tripService.findNearbyTrips(lat, lng, 10).pipe(catchError(() => of([] as Trip[])));
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (trips) => {
          this.requests.set(trips);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  goOnline(): void {
    this.setAvailability('AVAILABLE', 'You are now available for trips');
  }

  goOffline(): void {
    this.setAvailability('OFFLINE', 'You are now offline');
  }

  setDemoLocation(): void {
    this.updateLocation(37.7749, -122.4194, 'Demo location set (San Francisco)');
  }

  useBrowserLocation(): void {
    if (!navigator.geolocation) {
      this.snackBar.open('Geolocation not supported', 'Dismiss', { duration: 3000 });
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => this.updateLocation(pos.coords.latitude, pos.coords.longitude, 'Location updated'),
      () => this.snackBar.open('Could not read GPS location', 'Dismiss', { duration: 3000 })
    );
  }

  private setAvailability(status: DriverStatus, message: string): void {
    this.goingOnline.set(true);
    this.driverService.updateAvailability({ status }).subscribe({
      next: (profile) => {
        this.driver.set(profile);
        this.goingOnline.set(false);
        this.snackBar.open(message, 'OK', { duration: 2500 });
      },
      error: (err) => {
        this.goingOnline.set(false);
        this.snackBar.open(err?.error?.message || 'Could not update status', 'Dismiss', { duration: 3500 });
      }
    });
  }

  private updateLocation(lat: number, lng: number, message: string): void {
    this.updatingLocation.set(true);
    this.driverService.updateLocation({ lat, lng }).subscribe({
      next: (profile) => {
        this.driver.set(profile);
        this.updatingLocation.set(false);
        this.snackBar.open(message, 'OK', { duration: 2500 });
      },
      error: (err) => {
        this.updatingLocation.set(false);
        this.snackBar.open(err?.error?.message || 'Could not update location', 'Dismiss', { duration: 3500 });
      }
    });
  }

  accept(trip: Trip): void {
    this.acceptingId.set(trip.id);
    this.tripService.acceptTrip(trip.id).subscribe({
      next: (accepted) => {
        this.acceptingId.set(null);
        this.router.navigate(['/trips', accepted.id]);
      },
      error: (err) => {
        this.acceptingId.set(null);
        this.snackBar.open(err?.error?.message || 'Could not accept trip', 'Dismiss', { duration: 3500 });
      }
    });
  }

  isOnTrip(): boolean {
    return this.driver()?.status === 'ON_TRIP';
  }

  isAvailable(): boolean {
    return this.driver()?.status === 'AVAILABLE';
  }

  hasLocation(): boolean {
    const d = this.driver();
    return d?.currentLat != null && d?.currentLng != null;
  }
}
