import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import {
  CancelTripRequest,
  FareEstimateRequest,
  FareEstimateResponse,
  Trip,
  TripRequest,
  TripStatus
} from '../models/trip.model';

@Injectable({ providedIn: 'root' })
export class TripService {
  private readonly baseUrl = `${environment.apiBaseUrl}/trips`;

  constructor(private http: HttpClient) {}

  requestTrip(request: TripRequest): Observable<Trip> {
    return this.http.post<Trip>(this.baseUrl, request);
  }

  estimateFare(request: FareEstimateRequest): Observable<FareEstimateResponse> {
    return this.http.post<FareEstimateResponse>(`${this.baseUrl}/estimate-fare`, request);
  }

  listTrips(page: number, size: number, status?: TripStatus | null): Observable<PageResponse<Trip>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<PageResponse<Trip>>(this.baseUrl, { params });
  }

  getMyTrips(page: number, size: number, status?: TripStatus | null): Observable<PageResponse<Trip>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<PageResponse<Trip>>(`${this.baseUrl}/my`, { params });
  }

  getTripById(id: number): Observable<Trip> {
    return this.http.get<Trip>(`${this.baseUrl}/${id}`);
  }

  acceptTrip(id: number): Observable<Trip> {
    return this.http.post<Trip>(`${this.baseUrl}/${id}/accept`, {});
  }

  markArrived(id: number): Observable<Trip> {
    return this.http.post<Trip>(`${this.baseUrl}/${id}/arrive`, {});
  }

  startTrip(id: number): Observable<Trip> {
    return this.http.post<Trip>(`${this.baseUrl}/${id}/start`, {});
  }

  completeTrip(id: number, finalFare?: number): Observable<Trip> {
    return this.http.post<Trip>(`${this.baseUrl}/${id}/complete`, finalFare ? { finalFare } : {});
  }

  cancelTrip(id: number, request?: CancelTripRequest): Observable<Trip> {
    return this.http.post<Trip>(`${this.baseUrl}/${id}/cancel`, request ?? {});
  }

  submitReview(id: number, rating: number, comment?: string): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/${id}/review`, { rating, comment: comment ?? null });
  }
}
