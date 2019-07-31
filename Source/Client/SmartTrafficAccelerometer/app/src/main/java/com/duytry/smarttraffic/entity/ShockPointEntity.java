package com.duytry.smarttraffic.entity;

import com.duytry.smarttraffic.common.Common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ShockPointEntity {
    private int id;
    private RoadEntity roadEntity;
    private double latitude;
    private double longitude;
    private String timeCollect;
    private String nameFileData;


    public ShockPointEntity(int id, RoadEntity roadEntity, double latitude, double longitude, String timeCollect, String nameFileData) {
        this.id = id;
        this.roadEntity = roadEntity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeCollect = timeCollect;
        this.nameFileData = nameFileData;
        try {
            SimpleDateFormat dateInputFormat = new SimpleDateFormat(Common.DATE_INPUT_FORMAT);
            Date time = dateInputFormat.parse(timeCollect);
            SimpleDateFormat dateOutputFormat = new SimpleDateFormat(Common.DATE_OUTPUT_FORMAT);
            this.timeCollect = dateOutputFormat.format(time);
        } catch (ParseException e) {
        }
    }

    public ShockPointEntity(int id, double latitude, double longitude, String timeCollect, String nameFileData) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeCollect = timeCollect;
        this.nameFileData = nameFileData;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RoadEntity getRoadEntity() {
        return roadEntity;
    }

    public void setRoadEntity(RoadEntity roadEntity) {
        this.roadEntity = roadEntity;
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

    public String getTimeCollect() {
        return timeCollect;
    }

    public void setTimeCollect(String timeCollect) {
        this.timeCollect = timeCollect;
    }

    public String getNameFileData() {
        return nameFileData;
    }

    public void setNameFileData(String nameFileData) {
        this.nameFileData = nameFileData;
    }

    @Override
    public String toString() {
        StringBuilder shockPointInfo = new StringBuilder();
        shockPointInfo.append("id: ");
        shockPointInfo.append(id);
        shockPointInfo.append("\n");
        shockPointInfo.append("road name: ");
        shockPointInfo.append(roadEntity.getShortName());
        shockPointInfo.append("\n");
        shockPointInfo.append("position: ");
        shockPointInfo.append(latitude);
        shockPointInfo.append(", ");
        shockPointInfo.append(longitude);
        shockPointInfo.append("\n");
        shockPointInfo.append("time: ");
        shockPointInfo.append(timeCollect);
        return shockPointInfo.toString();
    }
}

