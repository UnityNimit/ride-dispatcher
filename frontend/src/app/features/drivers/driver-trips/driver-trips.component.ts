import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

import { TripService } from '../../../core/services/trip.service';
import { DriverService } from '../../../core/services/driver.service';
import { Trip, TripStatus } from '../../../core/models/trip.model';
import { TripStatusChipComponent } from '../../shared/components/trip-status-chip.component';
import { formatTripTimestamp } from '../../../core/utils/date-format.util';

const ACTIVE_STATUSES: TripStatus[] = ['ACCEPTED', 'ARRIVED', 'IN_PROGRESS'];

@Component({
  selector: 'app-driver-trips',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    TripStatusChipComponent
  ],
  templateUrl: './driver-trips.component.html',
  styleUrl: './driver-trips.component.scss'
})
export class DriverTripsComponent implements OnInit {
  private readonly tripService = inject(TripService);
  private readonly driverService = inject(DriverService);
  private readonly router = inject(Router);

  readonly displayedColumns = ['id', 'rider', 'status', 'requestedAt', 'fareEstimate', 'actions'];
  readonly formatTimestamp = formatTripTimestamp;

  readonly loading = signal(true);
  readonly trips = signal<Trip[]>([]);
  readonly activeTrip = signal<Trip | null>(null);
  readonly totalElements = signal(0);
  readonly pageSize = signal(10);
  readonly pageIndex = signal(0);

  ngOnInit(): void {
    this.driverService.getMyDriverProfile().subscribe({
      next: (profile) => this.loadTrips(profile.id),
      error: () => this.loading.set(false)
    });
  }

  private loadTrips(driverProfileId: number): void {
    this.loading.set(true);
    this.tripService.getDriverTrips(driverProfileId, this.pageIndex(), this.pageSize()).subscribe({
      next: (page) => {
        this.trips.set(page.content);
        this.totalElements.set(page.totalElements);
        this.activeTrip.set(
          page.content.find((t) => ACTIVE_STATUSES.includes(t.status)) ?? null
        );
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.driverService.getMyDriverProfile().subscribe({
      next: (profile) => this.loadTrips(profile.id)
    });
  }

  viewTrip(trip: Trip): void {
    this.router.navigate(['/trips', trip.id]);
  }

  isActive(status: TripStatus): boolean {
    return ACTIVE_STATUSES.includes(status);
  }
}
