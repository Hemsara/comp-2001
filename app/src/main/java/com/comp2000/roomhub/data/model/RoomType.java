package com.comp2000.roomhub.data.model;

public enum RoomType {
    MEETING("Meeting room"),
    SEMINAR("Seminar room");

    public final String label;

    RoomType(String label) {
        this.label = label;
    }
}
