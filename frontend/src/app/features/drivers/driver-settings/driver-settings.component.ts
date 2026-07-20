import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { DriverService } from '../../../core/services/driver.service';
import { DriverProfile, DriverStatus } from '../../../core/models/driver.model';

const DEMO_LAT = 37.7749;
const DEMO_LNG = -122.4194;

@Component({
  selector: 'app-driver-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './driver-settings.component.html',
  styleUrl: './driver-settings.component.scss'
})
export class DriverSettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly driverService = inject(DriverService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly profile = signal<DriverProfile | null>(null);

  readonly form = this.fb.group({
    vehicleMake: ['', Validators.required],
    vehicleModel: ['', Validators.required],
    vehicleColor: [''],
    licensePlate: ['', Validators.required]
  });

  ngOnInit(): void {
    this.driverService.getMyDriverProfile().subscribe({
      next: (p) => {
        this.profile.set(p);
        this.form.patchValue({
          vehicleMake: p.vehicleMake,
          vehicleModel: p.vehicleModel,
          vehicleColor: p.vehicleColor ?? '',
          licensePlate: p.licensePlate
        });
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  saveProfile(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.driverService.updateProfile({
      vehicleMake: v.vehicleMake!,
      vehicleModel: v.vehicleModel!,
      vehicleColor: v.vehicleColor || null,
      licensePlate: v.licensePlate!
    }).subscribe({
      next: (p) => {
        this.profile.set(p);
        this.saving.set(false);
        this.snackBar.open('Profile updated', 'OK', { duration: 2500 });
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(err?.error?.message || 'Could not update profile', 'Dismiss', { duration: 3500 });
      }
    });
  }

  setDemoLocation(): void {
    this.updateLocation(DEMO_LAT, DEMO_LNG, 'Demo location set (San Francisco)');
  }

  useBrowserLocation(): void {
    if (!navigator.geolocation) {
      this.snackBar.open('Geolocation not supported in this browser', 'Dismiss', { duration: 3000 });
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => this.updateLocation(pos.coords.latitude, pos.coords.longitude, 'Location updated from GPS'),
      () => this.snackBar.open('Could not read GPS location', 'Dismiss', { duration: 3000 })
    );
  }

  setAvailability(status: DriverStatus): void {
    this.saving.set(true);
    this.driverService.updateAvailability({ status }).subscribe({
      next: (p) => {
        this.profile.set(p);
        this.saving.set(false);
        this.snackBar.open(`Status set to ${status}`, 'OK', { duration: 2500 });
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(err?.error?.message || 'Could not update status', 'Dismiss', { duration: 3500 });
      }
    });
  }

  private updateLocation(lat: number, lng: number, message: string): void {
    this.saving.set(true);
    this.driverService.updateLocation({ lat, lng }).subscribe({
      next: (p) => {
        this.profile.set(p);
        this.saving.set(false);
        this.snackBar.open(message, 'OK', { duration: 2500 });
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(err?.error?.message || 'Could not update location', 'Dismiss', { duration: 3500 });
      }
    });
  }
}
