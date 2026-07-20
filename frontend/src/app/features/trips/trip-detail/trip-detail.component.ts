import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, startWith, switchMap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { TripService } from '../../../core/services/trip.service';
import { DashboardService } from '../../../core/services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { Trip } from '../../../core/models/trip.model';
import { TripStatusChipComponent } from '../../shared/components/trip-status-chip.component';
import { formatTripTimestamp } from '../../../core/utils/date-format.util';
import { environment } from '../../../../environments/environment';

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
export class TripDetailComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tripService = inject(TripService);
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

    // TODO: replace this interval-based poll with a real-time push channel
    // (WebSocket via STOMP, or Server-Sent Events) once the backend exposes
    // one. Polling is a deliberate placeholder - the signal-based `trip()`
    // state below is what a future push implementation would update instead.
    interval(environment.pollingIntervalMs)
      .pipe(
        startWith(0),
        switchMap(() => this.tripService.getTripById(this.tripId)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (trip) => {
          this.trip.set(trip);
          this.loading.set(false);
        },
        error: () => {
          this.notFound.set(true);
          this.loading.set(false);
        }
      });
  }

  goBack(): void {
    const role = this.currentRole();
    if (role === 'RIDER') {
      this.router.navigate(['/trip-history']);
    } else {
      this.router.navigate(['/trips']);
    }
  }

  canForceCancel(trip: Trip): boolean {
    return this.currentRole() === 'ADMIN'
      && trip.status !== 'COMPLETED'
      && trip.status !== 'CANCELLED';
  }

  canReview(trip: Trip): boolean {
    return this.currentRole() === 'RIDER'
      && trip.status === 'COMPLETED'
      && !this.reviewSubmitted();
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
