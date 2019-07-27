package com.example.smartTraffic.entity;

public class ShockingPointEntity {
    private int id;
    private double latitude;
    private double longitude;
    private RoadEntity roadEntity;

    public ShockingPointEntity(int id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public ShockingPointEntity(int id, double latitude, double longitude, RoadEntity roadEntity) {
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
        return "ShockingPointEntity{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", roadEntity=" + roadEntity +
                '}';
    }
}
