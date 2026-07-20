import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardStats } from '../models/dashboard.model';
import { Trip } from '../models/trip.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/dashboard-stats`);
  }

  forceCancelTrip(id: number, reason?: string): Observable<Trip> {
    let params = new HttpParams();
    if (reason) {
      params = params.set('reason', reason);
    }
    return this.http.post<Trip>(`${this.baseUrl}/trips/${id}/force-cancel`, null, { params });
  }
}
