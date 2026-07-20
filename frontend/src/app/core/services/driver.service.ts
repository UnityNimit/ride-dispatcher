import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import {
  DriverAvailabilityRequest,
  DriverLocationUpdateRequest,
  DriverProfile,
  DriverProfileUpdateRequest,
  DriverStatus
} from '../models/driver.model';

@Injectable({ providedIn: 'root' })
export class DriverService {
  private readonly baseUrl = `${environment.apiBaseUrl}/drivers`;

  constructor(private http: HttpClient) {}

  listDrivers(page: number, size: number, status?: DriverStatus | null): Observable<PageResponse<DriverProfile>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<PageResponse<DriverProfile>>(this.baseUrl, { params });
  }

  getDriver(id: number): Observable<DriverProfile> {
    return this.http.get<DriverProfile>(`${this.baseUrl}/${id}`);
  }

  getMyDriverProfile(): Observable<DriverProfile> {
    return this.http.get<DriverProfile>(`${this.baseUrl}/me`);
  }

  updateAvailability(request: DriverAvailabilityRequest): Observable<DriverProfile> {
    return this.http.patch<DriverProfile>(`${this.baseUrl}/me/availability`, request);
  }

  updateLocation(request: DriverLocationUpdateRequest): Observable<DriverProfile> {
    return this.http.patch<DriverProfile>(`${this.baseUrl}/me/location`, request);
  }

  updateProfile(request: DriverProfileUpdateRequest): Observable<DriverProfile> {
    return this.http.put<DriverProfile>(`${this.baseUrl}/me/profile`, request);
  }

  findNearby(lat: number, lng: number, radiusKm = 5): Observable<DriverProfile[]> {
    const params = new HttpParams()
      .set('lat', lat)
      .set('lng', lng)
      .set('radiusKm', radiusKm);
    return this.http.get<DriverProfile[]>(`${this.baseUrl}/nearby`, { params });
  }
}
