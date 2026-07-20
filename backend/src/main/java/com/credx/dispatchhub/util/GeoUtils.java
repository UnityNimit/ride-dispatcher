package com.credx.dispatchhub.util;

/**
 * Distance helpers used by fare estimation and nearby-driver search.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

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
}
