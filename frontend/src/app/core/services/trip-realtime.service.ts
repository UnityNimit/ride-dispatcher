import { Injectable, OnDestroy } from '@angular/core';
import { Observable, map } from 'rxjs';
import { RxStomp } from '@stomp/rx-stomp';
import SockJS from 'sockjs-client';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { Trip } from '../models/trip.model';

/**
 * STOMP/SockJS client for live trip updates on /topic/trips/{id}.
 */
@Injectable({ providedIn: 'root' })
export class TripRealtimeService implements OnDestroy {
  private rxStomp: RxStomp | null = null;

  constructor(private authService: AuthService) {}

  watchTrip(tripId: number): Observable<Trip> {
    this.ensureConnected();
    return this.rxStomp!.watch(`/topic/trips/${tripId}`).pipe(
      map((message) => JSON.parse(message.body) as Trip)
    );
  }

  deactivate(): void {
    if (this.rxStomp) {
      void this.rxStomp.deactivate();
      this.rxStomp = null;
    }
  }

  ngOnDestroy(): void {
    this.deactivate();
  }

  private ensureConnected(): void {
    if (this.rxStomp?.active) {
      return;
    }

    this.rxStomp = new RxStomp();
    const token = this.authService.getToken();
    this.rxStomp.configure({
      webSocketFactory: () => new SockJS(environment.wsUrl) as WebSocket,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 3000
    });
    this.rxStomp.activate();
  }
}
