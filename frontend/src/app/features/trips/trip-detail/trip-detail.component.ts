import { Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { TripService } from '../../../core/services/trip.service';
import { TripRealtimeService } from '../../../core/services/trip-realtime.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { Trip } from '../../../core/models/trip.model';
import { TripStatusChipComponent } from '../../shared/components/trip-status-chip.component';
import { formatTripTimestamp } from '../../../core/utils/date-format.util';

@Component({
  selector: 'app-trip-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    TripStatusChipComponent
  ],
  templateUrl: './trip-detail.component.html',
  styleUrl: './trip-detail.component.scss'
})
export class TripDetailComponent implements OnInit, OnDestroy {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tripService = inject(TripService);
  private readonly tripRealtime = inject(TripRealtimeService);
  private readonly dashboardService = inject(DashboardService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly trip = signal<Trip | null>(null);
  readonly actionBusy = signal(false);
  readonly reviewRating = signal(5);
  readonly reviewComment = signal('');
  readonly reviewSubmitted = signal(false);

  readonly formatTimestamp = formatTripTimestamp;
  readonly currentRole = this.authService.currentRole;

  private tripId!: number;

  ngOnInit(): void {
    this.tripId = Number(this.route.snapshot.paramMap.get('id'));

    if (!this.tripId) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }

    this.tripService.getTripById(this.tripId).subscribe({
      next: (trip) => {
        this.trip.set(trip);
        this.loading.set(false);
        this.subscribeToLiveUpdates();
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.tripRealtime.deactivate();
  }

  private subscribeToLiveUpdates(): void {
    this.tripRealtime
      .watchTrip(this.tripId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((trip) => this.trip.set(trip));
  }

  goBack(): void {
    const role = this.currentRole();
    if (role === 'RIDER') {
      this.router.navigate(['/trip-history']);
    } else if (role === 'DRIVER') {
      this.router.navigate(['/driver-trips']);
    } else {
      this.router.navigate(['/trips']);
    }
  }

  isDriver(): boolean {
    return this.currentRole() === 'DRIVER';
  }

  isRider(): boolean {
    return this.currentRole() === 'RIDER';
  }

  isAdmin(): boolean {
    return this.currentRole() === 'ADMIN';
  }

  canForceCancel(trip: Trip): boolean {
    return this.isAdmin()
      && trip.status !== 'COMPLETED'
      && trip.status !== 'CANCELLED';
  }

  canReview(trip: Trip): boolean {
    return this.isRider()
      && trip.status === 'COMPLETED'
      && !this.reviewSubmitted();
  }

  canCancel(trip: Trip): boolean {
    if (trip.status === 'COMPLETED' || trip.status === 'CANCELLED') {
      return false;
    }
    return this.isRider() || this.isDriver();
  }

  acceptTrip(): void {
    this.runAction(this.tripService.acceptTrip(this.tripId), 'Trip accepted');
  }

  markArrived(): void {
    this.runAction(this.tripService.markArrived(this.tripId), 'Marked as arrived');
  }

  startTrip(): void {
    this.runAction(this.tripService.startTrip(this.tripId), 'Trip started');
  }

  completeTrip(): void {
    this.runAction(this.tripService.completeTrip(this.tripId), 'Trip completed. You are now available for new requests.');
  }

  cancelTrip(): void {
    this.runAction(this.tripService.cancelTrip(this.tripId, { reason: 'Cancelled from trip detail' }), 'Trip cancelled');
  }

  private runAction(request$: Observable<Trip>, successMessage?: string): void {
    this.actionBusy.set(true);
    request$.subscribe({
      next: (trip) => {
        this.trip.set(trip);
        this.actionBusy.set(false);
        if (successMessage) {
          this.snackBar.open(successMessage, 'OK', { duration: 3000 });
        }
      },
      error: (err) => {
        this.actionBusy.set(false);
        this.snackBar.open(err?.error?.message || 'Action failed', 'Dismiss', { duration: 3500 });
      }
    });
  }

  forceCancel(): void {
    this.actionBusy.set(true);
    this.dashboardService.forceCancelTrip(this.tripId, 'Force-cancelled from ops dashboard').subscribe({
      next: (trip) => {
        this.trip.set(trip);
        this.actionBusy.set(false);
        this.snackBar.open('Trip force-cancelled', 'OK', { duration: 2500 });
      },
      error: (err) => {
        this.actionBusy.set(false);
        this.snackBar.open(err?.error?.message || 'Force-cancel failed', 'Dismiss', { duration: 3500 });
      }
    });
  }

  submitReview(): void {
    this.actionBusy.set(true);
    this.tripService.submitReview(this.tripId, this.reviewRating(), this.reviewComment() || undefined).subscribe({
      next: () => {
        this.reviewSubmitted.set(true);
        this.actionBusy.set(false);
        this.snackBar.open('Thanks for your review!', 'OK', { duration: 2500 });
      },
      error: (err) => {
        this.actionBusy.set(false);
        this.snackBar.open(err?.error?.message || 'Could not submit review', 'Dismiss', { duration: 3500 });
      }
    });
  }
}
