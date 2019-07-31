package com.duytry.smarttraffic.entity;

public class RoadEntity {
    private int id;
//    private String longName;
    private String shortName;

//    public RoadEntity(int id, String longName, String shortName) {
//        this.id = id;
//        this.longName = longName;
//        this.shortName = shortName;
//    }

    public RoadEntity(int id, String shortName) {
        this.id = id;
        this.shortName = shortName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

//    public String getLongName() {
//        return longName;
//    }
//
//    public void setLongName(String longName) {
//        this.longName = longName;
//    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
}
