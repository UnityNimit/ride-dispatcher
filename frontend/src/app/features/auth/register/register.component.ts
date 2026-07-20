import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/services/auth.service';
import { ApiErrorResponse } from '../../../core/models/dashboard.model';
import { UserRole } from '../../../core/models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly roles: { value: UserRole; label: string }[] = [
    { value: 'RIDER', label: 'Rider' },
    { value: 'DRIVER', label: 'Driver' }
  ];

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['RIDER' as UserRole, [Validators.required]]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();

    this.authService
      .register({
        fullName: raw.fullName!,
        email: raw.email!,
        phoneNumber: raw.phoneNumber!,
        password: raw.password!,
        role: raw.role!
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/dashboard']);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          const apiError = err.error as ApiErrorResponse | undefined;
          const fieldMessage = apiError?.fieldErrors?.[0]?.message;
          this.errorMessage.set(fieldMessage ?? apiError?.message ?? 'Unable to register. Please try again.');
        }
      });
  }
}
