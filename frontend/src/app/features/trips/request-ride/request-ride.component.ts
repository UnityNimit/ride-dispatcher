import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, switchMap, of, catchError } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TripService } from '../../../core/services/trip.service';
import { FareEstimateResponse } from '../../../core/models/trip.model';

/**
 * Pickup/dropoff coordinates are derived from typed addresses (hash-based
 * offsets around SF placeholders) so the fare preview changes as the rider
 * edits the form. Real geocoding is out of scope for this challenge.
 */
const PLACEHOLDER_PICKUP = { lat: 37.7749, lng: -122.4194 };
const PLACEHOLDER_DROPOFF = { lat: 37.7849, lng: -122.4094 };

function coordsFromAddress(address: string, base: { lat: number; lng: number }): { lat: number; lng: number } {
  let hash = 0;
  for (let i = 0; i < address.length; i++) {
    hash = (hash * 31 + address.charCodeAt(i)) | 0;
  }
  const latOffset = ((hash % 200) - 100) / 10_000;
  const lngOffset = (((hash >> 8) % 200) - 100) / 10_000;
  return { lat: base.lat + latOffset, lng: base.lng + lngOffset };
}

@Component({
  selector: 'app-request-ride',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './request-ride.component.html',
  styleUrl: './request-ride.component.scss'
})
export class RequestRideComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly tripService = inject(TripService);
  private readonly router = inject(Router);

  readonly form = this.fb.group({
    pickupAddress: ['', [Validators.required]],
    dropoffAddress: ['', [Validators.required]]
  });

  readonly fareEstimate = signal<FareEstimateResponse | null>(null);
  readonly estimating = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  ngOnInit(): void {
    this.form.valueChanges
      .pipe(
        debounceTime(350),
        distinctUntilChanged(
          (a, b) => a.pickupAddress === b.pickupAddress && a.dropoffAddress === b.dropoffAddress
        ),
        switchMap((value) => {
          const pickup = (value.pickupAddress ?? '').trim();
          const dropoff = (value.dropoffAddress ?? '').trim();
          if (!pickup || !dropoff) {
            this.fareEstimate.set(null);
            return of(null);
          }
          this.estimating.set(true);
          const pickupCoords = coordsFromAddress(pickup, PLACEHOLDER_PICKUP);
          const dropoffCoords = coordsFromAddress(dropoff, PLACEHOLDER_DROPOFF);
          return this.tripService
            .estimateFare({
              pickupLat: pickupCoords.lat,
              pickupLng: pickupCoords.lng,
              dropoffLat: dropoffCoords.lat,
              dropoffLng: dropoffCoords.lng
            })
            .pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((estimate) => {
        this.estimating.set(false);
        this.fareEstimate.set(estimate);
      });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);

    const raw = this.form.getRawValue();
    const pickupCoords = coordsFromAddress(raw.pickupAddress!.trim(), PLACEHOLDER_PICKUP);
    const dropoffCoords = coordsFromAddress(raw.dropoffAddress!.trim(), PLACEHOLDER_DROPOFF);

    this.tripService
      .requestTrip({
        pickupLat: pickupCoords.lat,
        pickupLng: pickupCoords.lng,
        pickupAddress: raw.pickupAddress!,
        dropoffLat: dropoffCoords.lat,
        dropoffLng: dropoffCoords.lng,
        dropoffAddress: raw.dropoffAddress!
      })
      .subscribe({
        next: (trip) => {
          this.submitting.set(false);
          this.router.navigate(['/trips', trip.id]);
        },
        error: (err) => {
          this.submitting.set(false);
          const message =
            err?.error?.message ||
            err?.error?.fieldErrors?.[0]?.message ||
            'Could not request ride. Please try again.';
          this.submitError.set(message);
        }
      });
  }
}
