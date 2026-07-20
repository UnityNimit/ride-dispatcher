export type DriverStatus = 'OFFLINE' | 'AVAILABLE' | 'ON_TRIP';

export interface DriverProfile {
  id: number;
  userId: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  vehicleMake: string;
  vehicleModel: string;
  vehicleColor: string | null;
  licensePlate: string;
  status: DriverStatus;
  currentLat: number | null;
  currentLng: number | null;
  rating: number;
  totalTrips: number;
}

export interface DriverAvailabilityRequest {
  status: DriverStatus;
}

export interface DriverLocationUpdateRequest {
  lat: number;
  lng: number;
}

export interface DriverProfileUpdateRequest {
  vehicleMake: string;
  vehicleModel: string;
  vehicleColor: string | null;
  licensePlate: string;
}
