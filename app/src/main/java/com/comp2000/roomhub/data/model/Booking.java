package com.comp2000.roomhub.data.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Booking {
    public final String id;
    public final String reference;
    public final Room room;
    public final String title;
    public final LocalDate date;
    public final LocalTime start;
    public final LocalTime end;
    public final User bookedBy;
    public final User bookedFor;
    public final int attendees;
    public BookingStatus status;

    public Booking(String id, String reference, Room room, String title, LocalDate date, LocalTime start, LocalTime end,
                   User bookedBy, User bookedFor, int attendees, BookingStatus status) {
        this.id = id;
        this.reference = reference;
        this.room = room;
        this.title = title;
        this.date = date;
        this.start = start;
        this.end = end;
        this.bookedBy = bookedBy;
        this.bookedFor = bookedFor;
        this.attendees = attendees;
        this.status = status;
    }

    public boolean overlaps(LocalDate d, LocalTime s, LocalTime e) {
        return date.equals(d) && start.isBefore(e) && s.isBefore(end);
    }

    public boolean isOnBehalf() {
        return !bookedBy.id.equals(bookedFor.id);
    }
}
