package com.comp2000.roomhub.data.model;

import java.time.LocalDateTime;

/** An employee request awaiting a manager decision: either a new booking or a cancellation. */
public class BookingRequest {
    public enum Type { BOOKING, CANCELLATION }

    public enum State { PENDING, APPROVED, DECLINED }

    public final String id;
    public final Type type;
    public final Booking booking;
    public final User requester;
    public final LocalDateTime submitted;
    public final String note;
    public State state = State.PENDING;
    public String declineReason;

    public BookingRequest(String id, Type type, Booking booking, User requester, LocalDateTime submitted, String note) {
        this.id = id;
        this.type = type;
        this.booking = booking;
        this.requester = requester;
        this.submitted = submitted;
        this.note = note;
    }
}
