import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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
import { PageResponse } from '../../../core/models/page.model';
import { DriverProfile, DriverStatus } from '../../../core/models/driver.model';
import { formatTripTimestamp } from '../../../core/utils/date-format.util';
import { environment } from '../../../../environments/environment';

const EMPTY_PAGE: PageResponse<Trip> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  last: true
};

@Component({
  selector: 'app-incoming-request',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './incoming-request.component.html',
  styleUrl: './incoming-request.component.scss'
})
export class IncomingRequestComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly acceptingId = signal<number | null>(null);
  readonly goingOnline = signal(false);
  readonly driver = signal<DriverProfile | null>(null);
  readonly requests = signal<Trip[]>([]);
  readonly formatTimestamp = formatTripTimestamp;

  constructor(
    private tripService: TripService,
    private driverService: DriverService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.driverService.getMyDriverProfile().subscribe({
      next: (profile) => this.driver.set(profile),
      error: () => this.snackBar.open('Could not load driver profile', 'Dismiss', { duration: 3000 })
    });

    interval(environment.pollingIntervalMs)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.tripService.listTrips(0, 20, 'REQUESTED').pipe(catchError(() => of(EMPTY_PAGE)))
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (page) => {
          this.requests.set(page.content);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  goOnline(): void {
    this.goingOnline.set(true);
    this.driverService.updateAvailability({ status: 'AVAILABLE' as DriverStatus }).subscribe({
      next: (profile) => {
        this.driver.set(profile);
        this.goingOnline.set(false);
        this.snackBar.open('You are now available for trips', 'OK', { duration: 2500 });
      },
      error: (err) => {
        this.goingOnline.set(false);
        this.snackBar.open(err?.error?.message || 'Could not go online', 'Dismiss', { duration: 3500 });
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

  isAvailable(): boolean {
    return this.driver()?.status === 'AVAILABLE';
  }
}
