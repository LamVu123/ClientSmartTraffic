package com.example.smartTraffic.entity;

import android.location.Location;

import java.util.Comparator;

public class ShockPointEntity {
    private int id;
    private double latitude;
    private double longitude;
    private RoadEntity roadEntity;

    public ShockPointEntity(int id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public ShockPointEntity(int id, double latitude, double longitude, RoadEntity roadEntity) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.roadEntity = roadEntity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public RoadEntity getRoadEntity() {
        return roadEntity;
    }

    public void setRoadEntity(RoadEntity roadEntity) {
        this.roadEntity = roadEntity;
    }

    @Override
    public String toString() {
        return "ShockPointEntity{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", roadEntity=" + roadEntity +
                '}';
    }

    public static class SortByDistance implements Comparator<ShockPointEntity>
    {
        private Location currentLocation;

        public SortByDistance(Location currentLocation) {
            this.currentLocation = currentLocation;
        }

        @Override
        public int compare(ShockPointEntity o1, ShockPointEntity o2) {
            Location shockPoint1 = new Location("shock point1");
            shockPoint1.setLatitude(o1.getLatitude());
            shockPoint1.setLongitude(o1.getLongitude());
            Location shockPoint2 = new Location("shock point2");
            shockPoint2.setLatitude(o2.getLatitude());
            shockPoint2.setLongitude(o2.getLongitude());

            float distance1 =  shockPoint1.distanceTo(currentLocation);
            float distance2 =  shockPoint2.distanceTo(currentLocation);
//            int output = (int) (distance1 - distance2);
//            if(output > 0){
//                return 1;
//            } else if(output == 0){
//                return 0;
//            } else {
//                return -1;
//            }
            return Float.compare(distance1, distance2);
        }
    }
}
