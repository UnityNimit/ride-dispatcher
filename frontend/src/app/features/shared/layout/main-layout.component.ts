import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

import { AuthService } from '../../../core/services/auth.service';

interface NavLink {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss'
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  readonly navLinks = computed<NavLink[]>(() => {
    const role = this.authService.currentRole();
    const links: NavLink[] = [{ label: 'Dashboard', path: '/dashboard', icon: 'dashboard' }];

    if (role === 'ADMIN') {
      links.push(
        { label: 'Trips', path: '/trips', icon: 'local_taxi' },
        { label: 'Drivers', path: '/drivers', icon: 'badge' }
      );
    }

    if (role === 'RIDER') {
      links.push(
        { label: 'Request a ride', path: '/request-ride', icon: 'add_road' },
        { label: 'My trip history', path: '/trip-history', icon: 'history' }
      );
    }

    if (role === 'DRIVER') {
      links.push({ label: 'Incoming requests', path: '/incoming-requests', icon: 'notifications_active' });
    }

    return links;
  });

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
