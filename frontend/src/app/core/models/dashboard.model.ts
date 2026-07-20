export interface DashboardStats {
  totalTripsToday: number;
  activeDrivers: number;
  completedTripsToday: number;
  cancelledTripsToday: number;
  tripsInProgress: number;
  totalRegisteredDrivers: number;
  totalRegisteredRiders: number;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
}

export interface DriverTripStats {
  driverId: number;
  driverName: string;
  completedTrips: number;
  totalRevenue: number;
  averageFare: number;
}
