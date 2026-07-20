package com.credx.dispatchhub.util;

/**
 * Distance helpers used by fare estimation and nearby-driver/trip search.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double KM_PER_DEGREE_LAT = 111.0;

    private GeoUtils() {
    }

    /**
     * Great-circle distance between two lat/lng points, in kilometers.
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double latDistance = lat2Rad - lat1Rad;
        double lngDistance = Math.toRadians(lng2) - Math.toRadians(lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Axis-aligned bounding box around a point for a given radius (km).
     * Used as an index-friendly pre-filter before Haversine.
     */
    public static BoundingBox boundingBox(double lat, double lng, double radiusKm) {
        double latDelta = radiusKm / KM_PER_DEGREE_LAT;
        double lngDelta = radiusKm / (KM_PER_DEGREE_LAT * Math.max(0.2, Math.cos(Math.toRadians(lat))));
        return new BoundingBox(lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta);
    }

    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
    }
}
