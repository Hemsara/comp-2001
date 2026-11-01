package com.comp2000.roomhub.data.model;

import java.util.List;

public class Room {
    public final String id;
    public final String name;
    public final String code;
    public final RoomType type;
    public final int capacity;
    public final String floor;
    public final List<String> amenities;

    public Room(String id, String name, String code, RoomType type, int capacity, String floor, List<String> amenities) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
        this.capacity = capacity;
        this.floor = floor;
        this.amenities = amenities;
    }

    /** e.g. "Cedar · Meeting 3". */
    public String displayName() {
        return name + " · " + (type == RoomType.MEETING ? "Meeting " : "Seminar ") + code.substring(1);
    }

    @Override
    public String toString() {
        return displayName() + " (" + capacity + " seats)";
    }
}
