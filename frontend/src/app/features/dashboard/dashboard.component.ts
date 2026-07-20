import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardStats } from '../../core/models/dashboard.model';
import { StatCardComponent } from '../shared/components/stat-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, MatIconModule, MatCardModule, StatCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly authService = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly stats = signal<DashboardStats | null>(null);

  readonly currentUser = this.authService.currentUser;

  ngOnInit(): void {
    // Only admins have access to /api/admin/dashboard-stats server-side;
    // riders/drivers get a lighter welcome view instead.
    if (this.authService.currentRole() !== 'ADMIN') {
      this.loading.set(false);
      return;
    }

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
  }
}
